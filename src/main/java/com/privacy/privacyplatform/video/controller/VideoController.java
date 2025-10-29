package com.privacy.privacyplatform.video.controller;

import com.privacy.privacyplatform.video.dto.request.InitUploadRequest;
import com.privacy.privacyplatform.video.dto.request.ProcessVideoRequest;
import com.privacy.privacyplatform.video.dto.response.InitUploadResponse;
import com.privacy.privacyplatform.video.dto.response.VideoResultResponse;
import com.privacy.privacyplatform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoService videoService;

    /**
     * 1. Pre-signed URL 생성
     * POST /api/videos/init-upload
     */
    @PostMapping("/init-upload")
    public ResponseEntity<InitUploadResponse> initUpload(
            @RequestBody InitUploadRequest request) {
        try {
            log.info("업로드 URL 요청: {}", request.getFilename());
            InitUploadResponse response = videoService.initUpload(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("업로드 URL 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 2. 비디오 처리 시작
     * POST /api/videos/{videoId}/process
     */
    @PostMapping("/{videoId}/process")
    public ResponseEntity<Void> processVideo(
            @PathVariable String videoId,
            @RequestBody ProcessVideoRequest request) {
        try {
            log.info("비디오 처리 요청: videoId={}", videoId);
            videoService.processVideo(videoId, request);
            return ResponseEntity.accepted().build();  // 202 Accepted
        } catch (Exception e) {
            log.error("비디오 처리 시작 실패: videoId={}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 3. 비디오 결과 조회
     * GET /api/videos/{videoId}
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResultResponse> getVideoResult(
            @PathVariable String videoId) {
        try {
            log.info("비디오 조회: videoId={}", videoId);
            VideoResultResponse response = videoService.getVideoResult(videoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("비디오 조회 실패: videoId={}", videoId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("비디오 조회 오류: videoId={}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 헬스 체크
     * GET /api/videos/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}