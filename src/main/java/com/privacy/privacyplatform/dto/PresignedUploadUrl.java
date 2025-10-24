package com.privacy.privacyplatform.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadUrl {
    private String uploadUrl;  // S3 업로드 URL
    private String s3Key;      // S3 경로
    private Integer expiresIn; // 유효 시간(초)
}
