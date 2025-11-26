package com.privacy.privacyplatform.video.controller;

import com.privacy.privacyplatform.external.ai.dto.AICallbackRequest;
import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.video.dto.request.InitUploadRequest;
import com.privacy.privacyplatform.video.dto.request.ProcessVideoRequest;
import com.privacy.privacyplatform.video.dto.response.InitUploadResponse;
import com.privacy.privacyplatform.video.dto.response.VideoResultResponse;
import com.privacy.privacyplatform.video.dto.response.VideoStatusResponse;
import com.privacy.privacyplatform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 비디오 업로드 초기화 (인증 필요)
     */
    @PostMapping("/init-upload")
    public ResponseEntity<InitUploadResponse> initUpload(
            @RequestBody InitUploadRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        log.info("📤 업로드 초기화: filename={}, userId={}", request.getFilename(), user.getUserId());

        InitUploadResponse response = videoService.initUpload(request, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 비디오 처리 시작 (인증 필요)
     */
    @PostMapping("/{videoId}/process")
    public ResponseEntity<Void> processVideo(
            @PathVariable String videoId,
            @RequestBody ProcessVideoRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("🎬 비디오 처리 시작: videoId={}, userId={}", videoId, user.getUserId());
        videoService.processVideo(videoId, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * ✅ 새로 추가: AI 서버 콜백 (인증 불필요 - 내부 통신)
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> handleAiCallback(@RequestBody AICallbackRequest request) {
        log.info("🤖 AI 콜백 수신: videoId={}", request.getVideoId());
        videoService.handleAiCallback(request);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 새로 추가: 비디오 상태 조회 (폴링용)
     */
    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(
            @PathVariable String videoId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("📡 상태 조회: videoId={}, userId={}", videoId, user.getUserId());
        VideoStatusResponse response = videoService.getVideoStatus(videoId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 비디오 결과 조회 (인증 필요)
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResultResponse> getVideoResult(
            @PathVariable String videoId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        log.info("📊 비디오 조회: videoId={}, userId={}", videoId, user.getUserId());

        VideoResultResponse response = videoService.getVideoResult(videoId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 내 비디오 목록 조회
     */
    @GetMapping("/my-videos")
    public ResponseEntity<List<VideoResultResponse>> getMyVideos(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("📋 내 비디오 목록 조회: userId={}", user.getUserId());

        List<VideoResultResponse> videos = videoService.getMyVideos(user.getUserId());
        return ResponseEntity.ok(videos);
    }

    /**
     * 비디오 삭제
     */
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable String videoId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        log.info("🗑️ 비디오 삭제: videoId={}, userId={}", videoId, user.getUserId());

        videoService.deleteVideo(videoId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Health Check (인증 불필요)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Video Service OK");
    }
}