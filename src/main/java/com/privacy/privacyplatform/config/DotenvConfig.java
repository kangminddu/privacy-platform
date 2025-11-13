package com.privacy.privacyplatform.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
public class DotenvConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();

            System.out.println("✅ .env 파일 로드 완료!");

            Properties props = new Properties();

            // ========== Database ==========
            props.setProperty("DB_HOST",
                    dotenv.get("DB_HOST", "localhost"));
            props.setProperty("DB_PORT",
                    dotenv.get("DB_PORT", "3306"));
            props.setProperty("DB_NAME",
                    dotenv.get("DB_NAME", "privacy_platform"));
            props.setProperty("DB_USERNAME",
                    dotenv.get("DB_USERNAME", "root"));
            props.setProperty("DB_PASSWORD",
                    dotenv.get("DB_PASSWORD", ""));

            // ========== JWT ==========
            props.setProperty("JWT_SECRET",
                    dotenv.get("JWT_SECRET", "default-secret-key-minimum-256-bits"));
            props.setProperty("JWT_ACCESS_EXPIRATION",
                    dotenv.get("JWT_ACCESS_EXPIRATION", "3600000"));
            props.setProperty("JWT_REFRESH_EXPIRATION",
                    dotenv.get("JWT_REFRESH_EXPIRATION", "604800000"));

            // ========== AI Server ==========
            props.setProperty("AI_SERVER_URL",
                    dotenv.get("AI_SERVER_URL", "http://localhost:5001"));

            // ========== AWS S3 ==========
            props.setProperty("AWS_S3_BUCKET_NAME",
                    dotenv.get("AWS_S3_BUCKET_NAME", ""));
            props.setProperty("AWS_S3_REGION",
                    dotenv.get("AWS_S3_REGION", "ap-northeast-2"));
            props.setProperty("AWS_ACCESS_KEY_ID",
                    dotenv.get("AWS_ACCESS_KEY_ID", ""));
            props.setProperty("AWS_SECRET_ACCESS_KEY",
                    dotenv.get("AWS_SECRET_ACCESS_KEY", ""));

            // ========== Email (Gmail) ==========
            props.setProperty("GMAIL_USERNAME",
                    dotenv.get("GMAIL_USERNAME", ""));
            props.setProperty("GMAIL_PASSWORD",
                    dotenv.get("GMAIL_PASSWORD", ""));

            // ========== OAuth2 - Kakao ==========
            props.setProperty("KAKAO_CLIENT_ID",
                    dotenv.get("KAKAO_CLIENT_ID", ""));
            props.setProperty("KAKAO_CLIENT_SECRET",
                    dotenv.get("KAKAO_CLIENT_SECRET", ""));

            // ========== Frontend URL ==========
            props.setProperty("FRONTEND_URL",
                    dotenv.get("FRONTEND_URL", "http://localhost:3000"));
            configurer.setProperties(props);

            System.out.println("📋 로드된 환경 변수:");
            System.out.println("  - DB_HOST: " + dotenv.get("DB_HOST", "localhost"));
            System.out.println("  - AWS_S3_BUCKET: " + dotenv.get("AWS_S3_BUCKET_NAME", "not set"));
            System.out.println("  - GMAIL_USERNAME: " + dotenv.get("GMAIL_USERNAME", "not set"));
            System.out.println("  - KAKAO_CLIENT_ID: " + dotenv.get("KAKAO_CLIENT_ID", "not set"));

        } catch (Exception e) {
            System.err.println("⚠️ .env 파일 로드 실패: " + e.getMessage());
            System.err.println("⚠️ 기본값으로 진행합니다.");
            configurer.setProperties(new Properties());
        }

        return configurer;
    }
}