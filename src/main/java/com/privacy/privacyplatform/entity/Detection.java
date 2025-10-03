package com.privacy.privacyplatform.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "detections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vided_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjectType objectType;

    private Integer frameNumber;
    private Integer timestampMs;

    @Column(columnDefinition = "TEXT")
    private String boundingBox;

    private Double confidence;
    private Boolean maskingApplied;

    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate(){
        detectedAt = LocalDateTime.now();
        if (maskingApplied == null){
            maskingApplied = false;
        }
    }
    public enum ObjectType {
        FACE,
        LICENSE_PLATE,
        SENSITIVE_TEXT,
        OTHER
    }
}
