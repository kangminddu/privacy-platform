package com.privacy.privacyplatform.external.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIProcessResponse {

    private String videoId;
    private String processedPath;
    private Integer frameCount;
    private Integer durationSeconds;
    private Integer fps;
    private List<DetectionResult> detections;
    private Long processingTimeMs;

    /**
     * 탐지 결과 (FastAPI → Spring Boot)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionResult {
        private String objectType;       // "FACE", "LICENSE_PLATE"
        private Float confidence;        // 0.0 ~ 1.0
        private BoundingBox boundingBox;
        private Integer frameNumber;
        private Integer timestampMs;
    }

    /**
     * Bounding Box
     */
    @Data  // ← 이것도!
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoundingBox {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
    }
}