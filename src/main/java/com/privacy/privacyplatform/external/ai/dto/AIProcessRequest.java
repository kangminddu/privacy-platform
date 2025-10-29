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
}
