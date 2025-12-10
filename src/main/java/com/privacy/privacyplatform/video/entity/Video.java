package com.privacy.privacyplatform.video.entity;

import com.privacy.privacyplatform.user.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "video_id", unique = true, nullable = false, length = 50)
    private String videoId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessStatus status;

    @Column(name = "s3_original_path", length = 500)
    private String s3OriginalPath;

    @Column(name = "s3_processed_path", length = 500)
    private String s3ProcessedPath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "frame_count")
    private Integer frameCount;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Detection> detections = new ArrayList<>();

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "unique_face_count")
    private Integer uniqueFaceCount;

    @Column(name = "unique_plate_count")
    private Integer uniquePlateCount;

    @Column(name = "unique_custom_count")
    private Integer uniqueCustomCount;

    @Column(name = "total_unique_objects")
    private Integer totalUniqueObjects;
    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        if (this.videoId == null) {
            this.videoId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = ProcessStatus.UPLOADED;
        }
    }

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