package com.privacy.privacyplatform.controller;

import com.privacy.privacyplatform.dto.UploadResponse;
import com.privacy.privacyplatform.dto.VideoResultResponse;
import com.privacy.privacyplatform.service.AiServerService;
import com.privacy.privacyplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final AiServerService aiServerService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        boolean aiHealthy = aiServerService.isHealthy();

        if (aiHealthy) {
            return ResponseEntity.ok("✅ 시스템 정상 (AI 서버 연결됨)");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("⚠️ AI 서버 연결 실패");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file) {

        log.info("업로드 요청: {} ({}KB)",
                file.getOriginalFilename(),
                file.getSize() / 1024);

        try {
            UploadResponse response = videoService.uploadAndProcess(file);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UploadResponse.builder()
                            .status("failed")
                            .message("업로드 실패: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResultResponse> getVideoResult(
            @PathVariable String videoId) {

        try {
            VideoResultResponse response = videoService.getVideoResult(videoId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("조회 실패: videoId={}", videoId, e);
            return ResponseEntity.notFound().build();
        }
    }
}