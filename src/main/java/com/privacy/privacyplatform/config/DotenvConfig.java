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

            // AWS S3
            props.setProperty("aws.s3.bucket-name",
                    dotenv.get("AWS_S3_BUCKET_NAME", ""));
            props.setProperty("aws.s3.region",
                    dotenv.get("AWS_S3_REGION", "ap-northeast-2"));
            props.setProperty("aws.access-key-id",
                    dotenv.get("AWS_ACCESS_KEY_ID", ""));
            props.setProperty("aws.secret-access-key",
                    dotenv.get("AWS_SECRET_ACCESS_KEY", ""));

            // JWT
            props.setProperty("jwt.secret",
                    dotenv.get("JWT_SECRET", "default-secret-key-change-in-production"));
            props.setProperty("jwt.access-token-expiration",
                    dotenv.get("JWT_ACCESS_TOKEN_EXPIRATION", "900000"));
            props.setProperty("jwt.refresh-token-expiration",
                    dotenv.get("JWT_REFRESH_TOKEN_EXPIRATION", "604800000"));

            configurer.setProperties(props);

        } catch (Exception e) {
            System.err.println("⚠️ .env 파일 로드 실패: " + e.getMessage());
            configurer.setProperties(new Properties());
        }

        return configurer;
    }
}