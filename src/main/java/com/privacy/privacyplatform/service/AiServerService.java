package com.privacy.privacyplatform.service;

import com.privacy.privacyplatform.dto.DetectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class AiServerService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final RestTemplate restTemplate;

    public AiServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isHealthy() {
        try {
            log.debug("AI 서버 헬스 체크: {}", aiServerUrl);
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    aiServerUrl + "/health",
                    Map.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("AI 서버 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    public DetectionResponse detectObjects(MultipartFile file) throws IOException {
        log.info("AI 서버로 이미지 전송: {}", file.getOriginalFilename());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<DetectionResponse> response = restTemplate.postForEntity(
                aiServerUrl + "/detect",
                requestEntity,
                DetectionResponse.class
        );

        log.info("AI 서버 응답 완료");
        return response.getBody();
    }
}