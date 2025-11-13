package com.privacy.privacyplatform.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@privacy-platform.com");
            message.setTo(toEmail);
            message.setSubject("[Privacy Platform] 이메일 인증 코드");
            message.setText(
                    "안녕하세요!\n\n" +
                            "Privacy Platform 회원가입 인증 코드입니다.\n\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "인증 코드: " + code + "\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                            "이 코드는 5분간 유효합니다.\n\n" +
                            "본인이 요청하지 않았다면 이 메일을 무시하세요.\n\n" +
                            "감사합니다.\n" +
                            "Privacy Platform 팀"
            );

            mailSender.send(message);
            log.info("✅ 인증 코드 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }
}