package com.privacy.privacyplatform.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        try {
            // .env 파일 로드
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")  // 프로젝트 루트
                    .ignoreIfMissing()  // .env 없어도 에러 안 남
                    .load();

            // 환경변수를 Spring Environment에 추가
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvMap.put(entry.getKey(), entry.getValue());
                // System property로도 설정 (선택사항)
                System.setProperty(entry.getKey(), entry.getValue());
            });

            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", dotenvMap));

            System.out.println("✅ .env 파일 로드 완료!");

        } catch (Exception e) {
            System.out.println("⚠️ .env 파일을 찾을 수 없습니다. 기본값을 사용합니다.");
        }
    }
}