package com.privacy.privacyplatform.external.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.domain.geo.BoundingBox;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIProcessResponse {

    private String videoId;
    private String processedPath;   // S3 processed/경로
    private Integer frameCount;     // 총 프레임 수
    private Integer durationSeconds; // 영상 길이(초)
    private Integer fps;           // FPS
    private List<DetectionResult> detections;
    private Long processingTimeMs; // 처리 시간 (밀리초)

    /**
     * 탐지 결과 (FastAPI -> Spring Boot)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionResult {
        private String objectType;
        private Float confidence;
        private BoundingBox boundingBox;
        private Integer frameNumber;
        private Integer timestampMs;
    }

    /**
     * Bouding Box
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
}

