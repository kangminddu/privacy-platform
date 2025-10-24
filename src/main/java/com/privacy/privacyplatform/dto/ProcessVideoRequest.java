package com.privacy.privacyplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessVideoRequest {
    private String s3Key;
    private Long fileSize;
}
