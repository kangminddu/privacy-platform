package com.privacy.privacyplatform.external.ai.service;

import com.privacy.privacyplatform.external.ai.dto.AIProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServerService {

    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    /**
     * ✅ AI 서버에 처리 요청 전송 (비동기 - 응답 안 기다림)
     */
    public void sendProcessRequest(AIProcessRequest request) {
        log.info("AI 서버에 요청 전송: videoId={}", request.getVideoId());

        webClient.post()
                .uri(aiServerUrl + "/api/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(v -> log.info("AI 서버 요청 전송 성공: videoId={}", request.getVideoId()))
                .doOnError(e -> log.error("AI 서버 요청 전송 실패: videoId={}", request.getVideoId(), e))
                .subscribe();  // ✅ 응답 기다리지 않음!
    }

    /**
     * AI 서버 헬스 체크
     */
    public boolean healthCheck() {
        try {
            String response = webClient.get()
                    .uri(aiServerUrl + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return "OK".equals(response);
        } catch (Exception e) {
            log.error("AI 서버 헬스 체크 실패", e);
            return false;
        }
    }
}