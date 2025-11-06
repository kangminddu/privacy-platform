package com.privacy.privacyplatform.video.repository;

import com.privacy.privacyplatform.video.entity.Video;
import com.privacy.privacyplatform.video.entity.enums.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByVideoId(String videoId);

    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<Video> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<Video> findByUserIdAndStatusOrderByUploadedAtDesc(Long userId, ProcessStatus status);

    List<Video> findByStatus(ProcessStatus status);

    boolean existsByVideoId(String videoId);
}