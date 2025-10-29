package com.privacy.privacyplatform.video.entity;

import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String videoId;  // UUID

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;  // video/mp4, video/avi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus status;

    @Column(length = 500)
    private String s3OriginalPath;

    @Column(length = 500)
    private String s3ProcessedPath;

    private Long fileSizeBytes;

    // 동영상 메타데이터
    private Integer durationSeconds;
    private Integer frameCount;
    private Integer fps;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Detection> detections = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.videoId == null) {
            this.videoId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = ProcessStatus.UPLOADED;
        }
    }

    // 편의 메서드
    public void addDetection(Detection detection) {
        detections.add(detection);
        detection.setVideo(this);
    }

    public void updateStatus(ProcessStatus status) {
        this.status = status;
        if (status == ProcessStatus.COMPLETED) {
            this.processedAt = LocalDateTime.now();
        }
    }
}