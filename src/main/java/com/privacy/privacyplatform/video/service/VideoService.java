package com.privacy.privacyplatform.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privacy.privacyplatform.external.ai.dto.AICallbackRequest;
import com.privacy.privacyplatform.external.ai.dto.AIProcessRequest;
import com.privacy.privacyplatform.external.ai.service.AIServerService;
import com.privacy.privacyplatform.storage.service.S3Service;
import com.privacy.privacyplatform.video.dto.request.InitUploadRequest;
import com.privacy.privacyplatform.video.dto.request.ProcessVideoRequest;
import com.privacy.privacyplatform.video.dto.response.InitUploadResponse;
import com.privacy.privacyplatform.video.dto.response.VideoResultResponse;
import com.privacy.privacyplatform.video.dto.response.VideoStatusResponse;
import com.privacy.privacyplatform.video.entity.Detection;
import com.privacy.privacyplatform.video.entity.Video;
import com.privacy.privacyplatform.video.entity.enums.ObjectType;
import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import com.privacy.privacyplatform.video.repository.VideoRepository;
import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final S3Service s3Service;
    private final AIServerService aiServerService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Value("${app.callback-url:https://api.safe-masking.cloud/api/videos/callback}")
    private String callbackUrl;

    /**
     * 1. 업로드 URL 생성 (Pre-signed URL)
     */
    @Transactional
    public InitUploadResponse initUpload(InitUploadRequest request, String userId) {
        log.info("업로드 초기화: filename={}, userId={}", request.getFilename(), userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Video video = Video.builder()
                .user(user)
                .originalFilename(request.getFilename())
                .contentType(request.getContentType())
                .status(ProcessStatus.UPLOADED)
                .build();

        videoRepository.save(video);

        var uploadUrl = s3Service.generatePresignedUploadUrl(
                request.getFilename(),
                request.getContentType()
        );

        log.info("비디오 생성 완료: videoId={}, userId={}", video.getVideoId(), userId);

        return InitUploadResponse.builder()
                .videoId(video.getVideoId())
                .uploadUrl(uploadUrl.getUrl())
                .s3Key(uploadUrl.getS3Key())
                .build();
    }

    /**
     * 2. 비디오 처리 시작 (비동기 - AI에 요청만 보내고 끝)
     */
    @Async
    @Transactional
    public void processVideo(String videoId, ProcessVideoRequest request) {
        log.info("비디오 처리 시작: videoId={}, options={}", videoId, request.getMaskingOptions());

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        video.setS3OriginalPath(request.getS3Key());
        video.setFileSizeBytes(request.getFileSize());
        video.updateStatus(ProcessStatus.PROCESSING);
        videoRepository.save(video);

        try {
            String downloadUrl = s3Service.generatePresignedDownloadUrl(request.getS3Key());

            String processedS3Key = "processed/masked_" + videoId + ".mp4";
            String uploadUrl = s3Service.generatePresignedUploadUrl(processedS3Key, "video/mp4").getUrl();
            video.setS3ProcessedPath(processedS3Key);
            videoRepository.save(video);
            ProcessVideoRequest.MaskingOptions opts = request.getMaskingOptions();

            AIProcessRequest aiRequest = AIProcessRequest.builder()
                    .downloadUrl(downloadUrl)
                    .uploadUrl(uploadUrl)
                    .videoId(videoId)
                    .callbackUrl(callbackUrl)
                    .maskingOptions(AIProcessRequest.MaskingOptions.builder()
                            .face(opts.getFace())
                            .licensePlate(opts.getLicensePlate())
                            .customObject(opts.getObject())
                            .customObjectName(opts.getObjectName())
                            .maskingOption_blur(!Boolean.TRUE.equals(opts.getUseAvatar()))
                            .maskingOption_swap(Boolean.TRUE.equals(opts.getUseAvatar()))
                            .build())
                    .build();

            aiServerService.sendProcessRequest(aiRequest);

            log.info("AI 서버에 처리 요청 전송 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.error("AI 서버 요청 실패: videoId={}", videoId, e);
            video.updateStatus(ProcessStatus.FAILED);
            videoRepository.save(video);
        }
    }

    @Transactional
    public void handleAiCallback(AICallbackRequest request) {
        log.info("AI 콜백 수신: videoId={}", request.getVideoId());

        Video video = videoRepository.findByVideoId(request.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found: " + request.getVideoId()));

        // ✅ maskedUrl은 무시 (이미 processVideo에서 s3ProcessedPath 설정됨)
        // video.setS3ProcessedPath(request.getMaskedUrl());

        video.setFrameCount(request.getFrameCount());
        video.setProcessingTimeMs(request.getProcessingTimeMs());
        video.updateStatus(ProcessStatus.COMPLETED);

        // 탐지 결과 저장
        if (request.getDetections() != null) {
            for (AICallbackRequest.DetectionResult item : request.getDetections()) {
                Detection detection = Detection.builder()
                        .video(video)
                        .classId(item.getClassId())
                        .label(item.getLabel())
                        .objectType(labelToObjectType(item.getLabel()))
                        .confidence(item.getConfidence())
                        .boundingBox(convertBboxToJson(item.getBbox()))
                        .frameNumber(item.getFrameNumber())
                        .maskingApplied(true)
                        .build();

                video.addDetection(detection);
            }
        }

        videoRepository.save(video);
        log.info("AI 콜백 처리 완료: videoId={}", request.getVideoId());
    }

    /**
     *  4. 비디오 상태 조회 (폴링용, 새로 추가)
     */
    public VideoStatusResponse getVideoStatus(String videoId, String userId) {
        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (!video.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 비디오만 조회할 수 있습니다");
        }

        String message = switch (video.getStatus()) {
            case UPLOADED -> "업로드 완료";
            case PROCESSING -> "AI 처리 중...";
            case COMPLETED -> "처리 완료";
            case FAILED -> "처리 실패";
        };

        return VideoStatusResponse.builder()
                .videoId(video.getVideoId())
                .status(video.getStatus())
                .message(message)
                .build();
    }

    /**
     * 5. 비디오 결과 조회
     */
    public VideoResultResponse getVideoResult(String videoId, String userId) {
        log.info("비디오 조회: videoId={}, userId={}", videoId, userId);

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (!video.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 비디오만 조회할 수 있습니다");
        }

        return buildVideoResultResponse(video);
    }

    /**
     * 6. 내 비디오 목록 조회
     */
    public List<VideoResultResponse> getMyVideos(String userId) {
        log.info("내 비디오 목록 조회: userId={}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<Video> videos = videoRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return videos.stream()
                .map(this::buildVideoResultResponse)
                .collect(Collectors.toList());
    }

    /**
     * 7. 비디오 삭제
     */
    @Transactional
    public void deleteVideo(String videoId, String userId) {
        log.info("비디오 삭제: videoId={}, userId={}", videoId, userId);

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (!video.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 비디오만 삭제할 수 있습니다");
        }

        if (video.getS3OriginalPath() != null) {
            s3Service.deleteFile(video.getS3OriginalPath());
        }
        if (video.getS3ProcessedPath() != null) {
            s3Service.deleteFile(video.getS3ProcessedPath());
        }

        videoRepository.delete(video);
        log.info("비디오 삭제 완료: videoId={}", videoId);
    }

    // ============== Helper 메서드 ==============

    /**
     * Video → VideoResultResponse 변환
     */
    private VideoResultResponse buildVideoResultResponse(Video video) {
        String originalUrl = video.getS3OriginalPath() != null
                ? s3Service.generatePresignedDownloadUrl(video.getS3OriginalPath())
                : null;

        String processedUrl = video.getS3ProcessedPath() != null
                ? s3Service.generatePresignedDownloadUrl(video.getS3ProcessedPath())
                : null;

        List<VideoResultResponse.DetectionDto> detectionDtos = video.getDetections().stream()
                .map(this::convertToDetectionDto)
                .collect(Collectors.toList());

        VideoResultResponse.DetectionStatistics statistics = calculateStatistics(video.getDetections());

        return VideoResultResponse.builder()
                .videoId(video.getVideoId())
                .originalFilename(video.getOriginalFilename())
                .status(video.getStatus())
                .originalDownloadUrl(originalUrl)
                .processedDownloadUrl(processedUrl)
                .fileSizeBytes(video.getFileSizeBytes())
                .frameCount(video.getFrameCount())
                .processingTimeMs(video.getProcessingTimeMs())
                .detections(detectionDtos)
                .statistics(statistics)
                .uploadedAt(video.getUploadedAt())
                .processedAt(video.getProcessedAt())
                .build();
    }

    /**
     * Detection → DetectionDto 변환
     */
    private VideoResultResponse.DetectionDto convertToDetectionDto(Detection detection) {
        return VideoResultResponse.DetectionDto.builder()
                .id(detection.getId())
                .classId(detection.getClassId())
                .label(detection.getLabel())
                .objectType(detection.getObjectType().name())
                .confidence(detection.getConfidence())
                .bbox(parseBbox(detection.getBoundingBox()))
                .frameNumber(detection.getFrameNumber())
                .timestampMs(detection.getTimestampMs())
                .maskingApplied(detection.getMaskingApplied())
                .build();
    }

    /**
     * Label → ObjectType 변환
     */
    private ObjectType labelToObjectType(String label) {
        if (label == null) return ObjectType.CUSTOM_OBJECT;

        return switch (label.toLowerCase()) {
            case "human head", "face" -> ObjectType.FACE;
            case "license plate" -> ObjectType.LICENSE_PLATE;
            default -> ObjectType.CUSTOM_OBJECT;
        };
    }

    /**
     * Bbox List → JSON 문자열
     */
    private String convertBboxToJson(List<Integer> bbox) {
        try {
            return objectMapper.writeValueAsString(bbox);
        } catch (JsonProcessingException e) {
            log.error("Bbox 변환 실패", e);
            return null;
        }
    }

    /**
     * JSON 문자열 → Bbox List
     */
    private List<Integer> parseBbox(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
        } catch (Exception e) {
            log.error("Bbox 파싱 실패", e);
            return null;
        }
    }

    /**
     * 탐지 통계 계산
     */
    private VideoResultResponse.DetectionStatistics calculateStatistics(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return VideoResultResponse.DetectionStatistics.builder()
                    .totalDetections(0L)
                    .faceCount(0L)
                    .licensePlateCount(0L)
                    .customObjectCount(0L)
                    .averageConfidence(0.0f)
                    .build();
        }

        long faceCount = detections.stream()
                .filter(d -> d.getObjectType() == ObjectType.FACE)
                .count();

        long licensePlateCount = detections.stream()
                .filter(d -> d.getObjectType() == ObjectType.LICENSE_PLATE)
                .count();

        long customObjectCount = detections.stream()
                .filter(d -> d.getObjectType() == ObjectType.CUSTOM_OBJECT)
                .count();

        float averageConfidence = (float) detections.stream()
                .mapToDouble(Detection::getConfidence)
                .average()
                .orElse(0.0);

        return VideoResultResponse.DetectionStatistics.builder()
                .totalDetections((long) detections.size())
                .faceCount(faceCount)
                .licensePlateCount(licensePlateCount)
                .customObjectCount(customObjectCount)
                .averageConfidence(averageConfidence)
                .build();
    }
}