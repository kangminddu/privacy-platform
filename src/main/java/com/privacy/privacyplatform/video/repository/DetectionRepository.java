package com.privacy.privacyplatform.video.repository;

import com.privacy.privacyplatform.video.entity.Detection;
import com.privacy.privacyplatform.video.entity.Video;
import com.privacy.privacyplatform.video.entity.enums.ObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {

    List<Detection> findByVideo(Video video);

    List<Detection> findByVideoAndObjectType(Video video, ObjectType objectType);

    Long countByVideo(Video video);
}