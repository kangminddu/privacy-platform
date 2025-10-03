package com.privacy.privacyplatform.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class UploadResponse {
    private String videoId;
    private String filename;
    private Long fileSizeBytes;
    private String status;
    private String message;
}
