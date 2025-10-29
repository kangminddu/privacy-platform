package com.privacy.privacyplatform.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitUploadResponse {
    private String videoId; // 생성된 비디오 ID (UUID)
    private String uploadUrl; // Pre-signed Upload URL (클라이언트가 S3에 직접 업로드)
    private String s3Key; // S3 키 (처리 시작 시 필요)
    private Integer expiresIn;
}
