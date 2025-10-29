package com.privacy.privacyplatform.video.dto.response;

import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResultResponse {

    private String videoId;
    private String originalFilename;
    private ProcessStatus status;

    // 다운로드 URL (Pre-signed)
    private String originalDownloadUrl;   // 원본 비디오
    private String processedDownloadUrl;  // 처리된 비디오

    // 비디오 메타데이터
    private Long fileSizeBytes;
    private Integer durationSeconds;
    private Integer frameCount;
    private Integer fps;

    // 탐지 결과
    private List<DetectionDto> detections;
    private DetectionStatistics statistics;

    // 타임스탬프
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    /**
     * 탐지 결과 DTO (내부 클래스)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionDto {
        private Long id;
        private String objectType;        // "FACE", "LICENSE_PLATE"
        private Float confidence;         // 0.0 ~ 1.0
        private BoundingBox boundingBox;
        private Integer frameNumber;
        private Integer timestampMs;
        private Boolean maskingApplied;
    }

    /**
     * Bounding Box DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoundingBox {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
    }

    /**
     * 탐지 통계 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionStatistics {
        private Long totalDetections;     // 총 탐지 수
        private Long faceCount;           // 얼굴 수
        private Long licensePlateCount;   // 번호판 수
        private Float averageConfidence;  // 평균 신뢰도
    }
}