package com.privacy.privacyplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class VideoResultResponse {
    private String videoId;
    private String originalFilename;
    private String status;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private List<DetectionSummary> detections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionSummary {
        private String type;
        private Integer count;
    }
}
