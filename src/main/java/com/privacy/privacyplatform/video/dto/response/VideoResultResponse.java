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
    private String originalDownloadUrl;
    private String processedDownloadUrl;

    // 비디오 메타데이터
    private Long fileSizeBytes;
    private Integer frameCount;

    // ✅ 추가: AI 처리 시간
    private Integer processingTimeMs;

    // 탐지 결과
    private List<DetectionDto> detections;
    private DetectionStatistics statistics;

    // 타임스탬프
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    /**
     * 탐지 결과 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionDto {
        private Long id;

        // ✅ 추가
        private Integer classId;
        private String label;

        private String objectType;
        private Float confidence;
        private List<Integer> bbox;  // ✅ 변경: [x1, x2, y1, y2]
        private Integer frameNumber;
        private Integer timestampMs;
        private Boolean maskingApplied;
    }

    /**
     * 탐지 통계 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionStatistics {
        private Long totalDetections;
        private Long faceCount;
        private Long licensePlateCount;

        // ✅ 추가
        private Long customObjectCount;

        private Float averageConfidence;
    }
}