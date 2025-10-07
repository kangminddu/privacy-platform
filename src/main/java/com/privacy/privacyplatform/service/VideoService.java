package com.privacy.privacyplatform.service;

import com.google.gson.Gson;
import com.privacy.privacyplatform.dto.DetectionResponse;
import com.privacy.privacyplatform.dto.UploadResponse;
import com.privacy.privacyplatform.dto.VideoResultResponse;
import com.privacy.privacyplatform.entity.Detection;
import com.privacy.privacyplatform.entity.Video;
import com.privacy.privacyplatform.repository.DetectionRepository;
import com.privacy.privacyplatform.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final DetectionRepository detectionRepository;
    private final AiServerService aiServerService;
    private final S3Service s3Service;
    private final Gson gson = new Gson();

    @Transactional
    public UploadResponse uploadAndProcess(MultipartFile file) throws Exception {

        // S3에 원본 업로드
        String s3Path = s3Service.uploadFile(file, "original");
        log.info("S3 경로: {}", s3Path);

        String videoId = UUID.randomUUID().toString();
        Video video = Video.builder()
                .videoId(videoId)
                .originalFilename(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .s3OriginalPath(s3Path)
                .status(Video.VideoStatus.PROCESSING)
                .build();

        videoRepository.save(video);
        log.info("DB 저장 완료: videoId={}", videoId);

        try {
            DetectionResponse aiResponse = aiServerService.detectObjects(file);

            if (aiResponse.getDetections() != null) {
                for (DetectionResponse.DetectionItem item : aiResponse.getDetections()) {
                    Detection detection = Detection.builder()
                            .video(video)
                            .objectType(Detection.ObjectType.valueOf(item.getType().toUpperCase()))
                            .boundingBox(gson.toJson(item.getBoundingBox()))
                            .confidence(item.getConfidence())
                            .maskingApplied(true)
                            .build();

                    detectionRepository.save(detection);
                }
            }

            video.setStatus(Video.VideoStatus.COMPLETED);
            video.setProcessedAt(LocalDateTime.now());
            videoRepository.save(video);

            log.info("처리 완료: videoId={}, 탐지 객체 수={}",
                    videoId, aiResponse.getDetections().size());

            return UploadResponse.builder()
                    .videoId(videoId)
                    .filename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .status("completed")
                    .message("처리가 완료되었습니다")
                    .build();

        } catch (Exception e) {
            log.error("처리 실패: videoId={}", videoId, e);
            video.setStatus(Video.VideoStatus.FAILED);
            videoRepository.save(video);

            throw new RuntimeException("AI 처리 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public VideoResultResponse getVideoResult(String videoId) {

        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new RuntimeException("비디오를 찾을 수 없습니다: " + videoId));

        List<Detection> detections = detectionRepository.findByVideoId(video.getId());

        Map<Detection.ObjectType, Long> countByType = detections.stream()
                .collect(Collectors.groupingBy(Detection::getObjectType, Collectors.counting()));

        List<VideoResultResponse.DetectionSummary> summaries = countByType.entrySet().stream()
                .map(entry -> VideoResultResponse.DetectionSummary.builder()
                        .type(entry.getKey().name())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(Collectors.toList());

        return VideoResultResponse.builder()
                .videoId(video.getVideoId())
                .originalFilename(video.getOriginalFilename())
                .status(video.getStatus().name())
                .fileSizeBytes(video.getFileSizeBytes())
                .uploadedAt(video.getUploadedAt())
                .processedAt(video.getProcessedAt())
                .detections(summaries)
                .build();
    }
}