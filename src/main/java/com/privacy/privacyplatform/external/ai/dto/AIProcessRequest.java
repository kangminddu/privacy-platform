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

    private String downloadUrl;
    private String videoId;
    private String uploadUrl;
    private String callbackUrl;

    private MaskingOptions maskingOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaskingOptions {
        private Boolean face;
        private Boolean licensePlate;

        private Boolean customObject;

        private String customObjectName;

        @Builder.Default
        private Boolean maskingOption_blur = true;

        @Builder.Default
        private Boolean maskingOption_swap = false;
    }
}