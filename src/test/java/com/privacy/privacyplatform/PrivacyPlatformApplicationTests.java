package com.privacy.privacyplatform;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@ActiveProfiles("test")
class PrivacyPlatformApplicationTests {

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }

        @Bean
        @Primary
        public S3Client s3Client() {
            return Mockito.mock(S3Client.class);
        }
    }

    @Test
    void contextLoads() {
        // Spring Context 로딩 테스트
    }

}