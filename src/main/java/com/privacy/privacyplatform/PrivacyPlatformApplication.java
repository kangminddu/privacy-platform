package com.privacy.privacyplatform;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrivacyPlatformApplication {

    public static void main(String[] args) {
        // .env 파일 로드
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        // 시스템 프로퍼티 설정
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(PrivacyPlatformApplication.class, args);
    }

}
// Docker Hub 인증 수정 테스트
// EC2 보안 그룹 수정 후 재배포
// 배포 테스트 2025-11-17
