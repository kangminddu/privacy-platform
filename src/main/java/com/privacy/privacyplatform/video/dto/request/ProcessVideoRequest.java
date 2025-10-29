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
    private String s3Key; // S3에 저장된 파일 경로 (예 : "original/uuid_video.mp4")
    private Long fileSize; // 파일 크기 (bytes)
}
