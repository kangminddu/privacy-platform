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
public class AICallbackRequest {

    private String videoId;

    private String maskedUrl;

    private Integer frameCount;
    private Integer processingTimeMs;

    private List<DetectionResult> detections;

    private Statistics statistics;


    /**
     * 탐지 결과
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionResult {
        private Integer frameNumber;

        private Integer classId;
        private String label;

        private Float confidence;

        private List<Integer> bbox;  // [x1, x2, y1, y2]
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        private Integer totalUniqueObjects;
        private Integer faceCount;
        private Integer licensePlateCount;
        private Integer customObjectCount;
        private Integer totalDetectionEvents;
        private Float averageConfidence;
    }
}