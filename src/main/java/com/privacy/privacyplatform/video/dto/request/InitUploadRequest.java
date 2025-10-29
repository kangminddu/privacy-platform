package com.privacy.privacyplatform.video.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitUploadRequest {

    private String filename; // 원본 파일명 (예 : "video.mp4")
    private String contentType; // MIME 타입 (예 : "video/mp4")

}
