package com.privacy.privacyplatform.external.ai.service;

import com.privacy.privacyplatform.external.ai.dto.AIProcessRequest;
import com.privacy.privacyplatform.external.ai.dto.AIProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServerService {

    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;  // http://localhost:5001

    /**
     * FastAPI에 비디오 처리 요청
     */
    public AIProcessResponse processVideo(AIProcessRequest request) {
        log.info("AI 서버 호출: videoId={}", request.getVideoId());

        try {
            AIProcessResponse response = webClient.post()
                    .uri(aiServerUrl + "/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AIProcessResponse.class)
                    .timeout(Duration.ofMinutes(30))  // 30분 타임아웃
                    .block();

            log.info("AI 처리 완료: videoId={}, detections={}",
                    response.getVideoId(),
                    response.getDetections().size());

            return response;

        } catch (Exception e) {
            log.error("AI 서버 호출 실패: videoId={}", request.getVideoId(), e);
            throw new RuntimeException("AI 서버 호출 실패", e);
        }
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