package com.privacy.privacyplatform.auth;

import com.privacy.privacyplatform.user.LoginAttempt;
import com.privacy.privacyplatform.user.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final LoginAttemptRepository loginAttemptRepository;

    // 로그인 시도 제한 확인 (15분 내 5회)
    public void checkLoginAttempts(String email, String ipAddress) {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        int attempts = loginAttemptRepository
                .countByEmailAndIpAddressAndAttemptedAtAfter(
                        email,
                        ipAddress,
                        fifteenMinutesAgo
                );

        if (attempts >= 5) {
            log.warn("⚠️ Too many login attempts: email={}, ip={}", email, ipAddress);
            throw new RuntimeException("너무 많은 로그인 시도입니다. 15분 후 다시 시도해주세요.");
        }
    }

    // 로그인 시도 기록
    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean success) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build();

        loginAttemptRepository.save(attempt);
    }

    // 오래된 로그인 시도 기록 삭제 (30일 이상)
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        loginAttemptRepository.deleteOldAttempts(thirtyDaysAgo);
        log.info("✅ 오래된 로그인 시도 기록 삭제 완료");
    }
}