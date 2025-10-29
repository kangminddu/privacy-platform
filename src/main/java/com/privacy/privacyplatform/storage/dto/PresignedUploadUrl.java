package com.privacy.privacyplatform.storage.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadUrl {
    private String url;  // S3 업로드 URL
    private String s3Key;      // S3 경로
}
