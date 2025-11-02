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

    @Column(name = "video_id", unique = true, nullable = false, length = 50)
    private String videoId;  // UUID

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;  // video/mp4, video/avi

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessStatus status;

    // ✅ DB 컬럼명과 정확히 맞춤
    @Column(name = "s3_original_path", length = 500)
    private String s3OriginalPath;

    @Column(name = "s3_processed_path", length = 500)
    private String s3ProcessedPath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    // 동영상 메타데이터
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "frame_count")
    private Integer frameCount;

    @Column(name = "fps")
    private Integer fps;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Detection> detections = new ArrayList<>();

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
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