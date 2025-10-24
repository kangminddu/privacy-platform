package com.privacy.privacyplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitUploadResponse {
    private String videoId;
    private String uploadUrl;
    private String s3Key;
    private Integer expiresIn;
}
