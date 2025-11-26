package com.privacy.privacyplatform.video.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessVideoRequest {

    private String s3Key;
    private Long fileSize;

    private MaskingOptions maskingOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaskingOptions {
        private Boolean face;
        private Boolean licensePlate;
        private Boolean object;

        private String objectName;

        private Boolean useAvatar;
    }
}