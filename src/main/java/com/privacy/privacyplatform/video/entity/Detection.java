package com.privacy.privacyplatform.video.entity;

import com.privacy.privacyplatform.video.entity.enums.ObjectType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "detections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 30)
    private ObjectType objectType;

    @Column(name = "confidence", nullable = false)
    private Float confidence;  // 0.0 ~ 1.0

    @Column(name = "bounding_box", columnDefinition = "TEXT")
    private String boundingBox;  // JSON: {"x": 100, "y": 200, "width": 50, "height": 50}

    @Column(name = "frame_number")
    private Integer frameNumber;

    @Column(name = "timestamp_ms")
    private Integer timestampMs;

    @Column(name = "masking_applied", nullable = false)
    @Builder.Default
    private Boolean maskingApplied = false;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        this.detectedAt = LocalDateTime.now();
        if (this.maskingApplied == null) {
            this.maskingApplied = false;
        }
    }
}