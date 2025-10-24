package com.privacy.privacyplatform.controller;

import com.privacy.privacyplatform.dto.*;
import com.privacy.privacyplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS 허용
public class VideoController {

    private final VideoService videoService;

    /**
     * 방법 1: 직접 업로드 (기존 방식)
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDirect(@RequestParam("file") MultipartFile file) {
        try {
            UploadResponse response = videoService.uploadAndProcess(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("업로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방법 2-1: Pre-signed URL 생성 (새로운 방식)
     */
    @PostMapping("/init-upload")
    public ResponseEntity<InitUploadResponse> initUpload(@RequestBody PresignedUrlRequest request) {
        try {
            InitUploadResponse response = videoService.initUpload(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방법 2-2: 업로드 완료 후 처리 시작
     */
    @PostMapping("/{videoId}/process")
    public ResponseEntity<Void> processVideo(
            @PathVariable String videoId,
            @RequestBody ProcessVideoRequest request) {
        try {
            videoService.processVideo(videoId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("비디오 처리 시작 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 비디오 결과 조회
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResultResponse> getVideoResult(@PathVariable String videoId) {
        try {
            VideoResultResponse response = videoService.getVideoResult(videoId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비디오 조회 실패", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}