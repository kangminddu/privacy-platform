package com.privacy.privacyplatform.external.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIProcessRequest {

    private String downloadUrl; // S3 Pre-signed Download URL
    private String videoId; // 비디오 ID

    private MaskingOptions maskingOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaskingOptions {
        private Boolean face;         // 얼굴 마스킹 여부
        private Boolean licensePlate; // 번호판 마스킹 여부
        private Boolean object;       // 객체 마스킹 여부
    }
}