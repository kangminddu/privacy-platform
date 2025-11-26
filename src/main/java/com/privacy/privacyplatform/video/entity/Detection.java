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

    // ✅ 추가: 객체별 고유 ID
    @Column(name = "class_id")
    private Integer classId;

    // ✅ 추가: 탐지된 객체 라벨
    @Column(name = "label", length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 30)
    private ObjectType objectType;

    @Column(name = "confidence", nullable = false)
    private Float confidence;

    @Column(name = "bounding_box", columnDefinition = "TEXT")
    private String boundingBox;  // JSON: [x1, x2, y1, y2]

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