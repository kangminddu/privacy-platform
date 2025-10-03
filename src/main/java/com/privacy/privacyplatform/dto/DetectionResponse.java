package com.privacy.privacyplatform.dto;

import lombok.Data;

import java.util.List;

@Data
public class DetectionResponse {
    private String status;
    private List<DetectionItem> detections;
    private String originalFilename;
    private String resultFilename;
    private String downloadUrl;
    private Double processingTimeSeconds;

    @Data
    public static class DetectionItem {
        private Integer id;
        private String type;
        private BoundingBox boundingBox;
        private Double confidence;
    }

    @Data
    public static class BoundingBox {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
    }
}
