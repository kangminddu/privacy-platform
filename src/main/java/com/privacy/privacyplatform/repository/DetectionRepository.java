package com.privacy.privacyplatform.repository;

import com.privacy.privacyplatform.entity.Detection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {
    List<Detection> findByVideoId(Long videoId);
}