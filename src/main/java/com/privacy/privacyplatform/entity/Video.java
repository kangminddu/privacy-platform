package com.privacy.privacyplatform.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String videoId;

    @Column(nullable = false)
    private String originalFilename;

    private String s3OriginalPath;
    private String s3ProcessedPath;
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate(){
        uploadedAt = LocalDateTime.now();
    }

    public enum VideoStatus{
        UPLOADED, PROCESSING, COMPLETED, FAILED
    }

}

