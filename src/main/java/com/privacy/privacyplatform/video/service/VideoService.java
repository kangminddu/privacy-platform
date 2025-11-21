package com.privacy.privacyplatform.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privacy.privacyplatform.external.ai.dto.AIProcessRequest;
import com.privacy.privacyplatform.external.ai.dto.AIProcessResponse;
import com.privacy.privacyplatform.external.ai.service.AIServerService;
import com.privacy.privacyplatform.storage.dto.PresignedUploadUrl;
import com.privacy.privacyplatform.storage.service.S3Service;
import com.privacy.privacyplatform.video.dto.request.InitUploadRequest;
import com.privacy.privacyplatform.video.dto.request.ProcessVideoRequest;
import com.privacy.privacyplatform.video.dto.response.InitUploadResponse;
import com.privacy.privacyplatform.video.dto.response.VideoResultResponse;
import com.privacy.privacyplatform.video.entity.Detection;
import com.privacy.privacyplatform.video.entity.Video;
import com.privacy.privacyplatform.video.entity.enums.ObjectType;
import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import com.privacy.privacyplatform.video.repository.DetectionRepository;
import com.privacy.privacyplatform.video.repository.VideoRepository;
import com.privacy.privacyplatform.websocket.dto.ProgressMessage;
import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final DetectionRepository detectionRepository;
    private final S3Service s3Service;
    private final AIServerService aiServerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    /**
     * 1. 업로드 URL 생성 (Pre-signed URL)
     *  userId 파라미터 추가
     */
    @Transactional
    public InitUploadResponse initUpload(InitUploadRequest request, String userId) {
        log.info("업로드 초기화: filename={}, userId={}", request.getFilename(), userId);

        // ⭐ 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // Video 엔티티 생성
        Video video = Video.builder()
                .user(user)
                .originalFilename(request.getFilename())
                .contentType(request.getContentType())
                .status(ProcessStatus.UPLOADED)
                .build();

        videoRepository.save(video);

        // S3 Upload URL 생성
        PresignedUploadUrl uploadUrl = s3Service.generatePresignedUploadUrl(
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
     * 2. 비디오 처리 시작 (비동기)
     * - 변경 없음 (그대로 유지)
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

        sendProgress(videoId, 0, "PROCESSING", "AI 처리 시작");

        try {
            String downloadUrl = s3Service.generatePresignedDownloadUrl(request.getS3Key());

            // ⭐ 마스킹 옵션 포함
            AIProcessRequest aiRequest = AIProcessRequest.builder()
                    .downloadUrl(downloadUrl)
                    .videoId(videoId)
                    .maskingOptions(AIProcessRequest.MaskingOptions.builder()
                            .face(request.getMaskingOptions().getFace())
                            .licensePlate(request.getMaskingOptions().getLicensePlate())
                            .object(request.getMaskingOptions().getObject())
                            .build())
                    .build();

            AIProcessResponse aiResponse = aiServerService.processVideo(aiRequest);

            saveProcessingResult(video, aiResponse);
            sendProgress(videoId, 100, "COMPLETED", "처리 완료");

            log.info("비디오 처리 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.error("비디오 처리 실패: videoId={}", videoId, e);
            video.updateStatus(ProcessStatus.FAILED);
            videoRepository.save(video);
            sendProgress(videoId, 0, "FAILED", "처리 실패: " + e.getMessage());
        }
    }

    /**
     * 3. 비디오 결과 조회
     *  userId 파라미터 추가 (권한 체크)
     */
    public VideoResultResponse getVideoResult(String videoId, String userId) {
        log.info("비디오 조회: videoId={}, userId={}", videoId, userId);

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        //  권한 체크 (본인의 비디오만 조회 가능)
        if (!video.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 비디오만 조회할 수 있습니다");
        }

        // Download URL 생성
        String originalUrl = video.getS3OriginalPath() != null
                ? s3Service.generatePresignedDownloadUrl(video.getS3OriginalPath())
                : null;

        String processedUrl = video.getS3ProcessedPath() != null
                ? s3Service.generatePresignedDownloadUrl(video.getS3ProcessedPath())
                : null;

        // 탐지 결과 변환
        List<VideoResultResponse.DetectionDto> detectionDtos = video.getDetections().stream()
                .map(this::convertToDetectionDto)
                .collect(Collectors.toList());

        // 통계 계산
        VideoResultResponse.DetectionStatistics statistics = calculateStatistics(video.getDetections());

        return VideoResultResponse.builder()
                .videoId(video.getVideoId())
                .originalFilename(video.getOriginalFilename())
                .status(video.getStatus())
                .originalDownloadUrl(originalUrl)
                .processedDownloadUrl(processedUrl)
                .fileSizeBytes(video.getFileSizeBytes())
                .durationSeconds(video.getDurationSeconds())
                .frameCount(video.getFrameCount())
                .fps(video.getFps())
                .detections(detectionDtos)
                .statistics(statistics)
                .uploadedAt(video.getUploadedAt())
                .processedAt(video.getProcessedAt())
                .build();
    }

    /**
     *  4. 내 비디오 목록 조회 (새로 추가!)
     */
    public List<VideoResultResponse> getMyVideos(String userId) {
        log.info("내 비디오 목록 조회: userId={}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<Video> videos = videoRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return videos.stream()
                .map(video -> {
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
                            .durationSeconds(video.getDurationSeconds())
                            .frameCount(video.getFrameCount())
                            .fps(video.getFps())
                            .detections(detectionDtos)
                            .statistics(statistics)
                            .uploadedAt(video.getUploadedAt())
                            .processedAt(video.getProcessedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     *  5. 비디오 삭제 (새로 추가!)
     */
    @Transactional
    public void deleteVideo(String videoId, String userId) {
        log.info("비디오 삭제: videoId={}, userId={}", videoId, userId);

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        // 권한 체크
        if (!video.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 비디오만 삭제할 수 있습니다");
        }

        // S3에서 삭제
        if (video.getS3OriginalPath() != null) {
            s3Service.deleteFile(video.getS3OriginalPath());
        }
        if (video.getS3ProcessedPath() != null) {
            s3Service.deleteFile(video.getS3ProcessedPath());
        }

        // DB에서 삭제
        videoRepository.delete(video);
        log.info("비디오 삭제 완료: videoId={}", videoId);
    }

    // ============== 아래는 기존 메서드 그대로 유지 ==============

    /**
     * AI 처리 결과 저장
     */
    private void saveProcessingResult(Video video, AIProcessResponse response) {
        video.setS3ProcessedPath(response.getProcessedPath());
        video.setFrameCount(response.getFrameCount());
        video.setDurationSeconds(response.getDurationSeconds());
        video.setFps(response.getFps());
        video.updateStatus(ProcessStatus.COMPLETED);

        if (response.getDetections() != null) {
            for (AIProcessResponse.DetectionResult aiDetection : response.getDetections()) {
                Detection detection = Detection.builder()
                        .video(video)
                        .objectType(ObjectType.valueOf(aiDetection.getObjectType()))
                        .confidence(aiDetection.getConfidence())
                        .boundingBox(convertBoundingBoxToJson(aiDetection.getBoundingBox()))
                        .frameNumber(aiDetection.getFrameNumber())
                        .timestampMs(aiDetection.getTimestampMs())
                        .maskingApplied(true)
                        .build();

                video.addDetection(detection);
            }
        }

        videoRepository.save(video);
    }

    /**
     * WebSocket으로 진행 상황 전송
     */
    private void sendProgress(String videoId, int percentage, String status, String message) {
        ProgressMessage progress = ProgressMessage.builder()
                .videoId(videoId)
                .percentage(percentage)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/progress/" + videoId, progress);
    }

    /**
     * Detection → DetectionDto 변환
     */
    private VideoResultResponse.DetectionDto convertToDetectionDto(Detection detection) {
        return VideoResultResponse.DetectionDto.builder()
                .id(detection.getId())
                .objectType(detection.getObjectType().name())
                .confidence(detection.getConfidence())
                .boundingBox(parseBoundingBox(detection.getBoundingBox()))
                .frameNumber(detection.getFrameNumber())
                .timestampMs(detection.getTimestampMs())
                .maskingApplied(detection.getMaskingApplied())
                .build();
    }

    /**
     * Bounding Box JSON 파싱
     */
    private VideoResultResponse.BoundingBox parseBoundingBox(String json) {
        try {
            return objectMapper.readValue(json, VideoResultResponse.BoundingBox.class);
        } catch (Exception e) {
            log.error("Bounding Box 파싱 실패", e);
            return null;
        }
    }

    /**
     * Bounding Box 객체 → JSON 문자열
     */
    private String convertBoundingBoxToJson(AIProcessResponse.BoundingBox bbox) {
        try {
            return objectMapper.writeValueAsString(bbox);
        } catch (Exception e) {
            log.error("Bounding Box 변환 실패", e);
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
                    .averageConfidence(0.0f)
                    .build();
        }

        long faceCount = detections.stream()
                .filter(d -> d.getObjectType() == ObjectType.FACE)
                .count();

        long licensePlateCount = detections.stream()
                .filter(d -> d.getObjectType() == ObjectType.LICENSE_PLATE)
                .count();

        float averageConfidence = (float) detections.stream()
                .mapToDouble(Detection::getConfidence)
                .average()
                .orElse(0.0);

        return VideoResultResponse.DetectionStatistics.builder()
                .totalDetections((long) detections.size())
                .faceCount(faceCount)
                .licensePlateCount(licensePlateCount)
                .averageConfidence(averageConfidence)
                .build();
    }
}