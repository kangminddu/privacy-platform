package com.privacy.privacyplatform.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressMessage {
    private String videoId;
    private Integer percentage;
    private String status;
    private String message;
    private LocalDateTime timestamp;

}
