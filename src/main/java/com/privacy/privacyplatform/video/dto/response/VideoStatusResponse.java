package com.privacy.privacyplatform.video.dto.response;

import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoStatusResponse {
    private String videoId;
    private ProcessStatus status;
    private String message;
}