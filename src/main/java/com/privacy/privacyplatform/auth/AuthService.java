package com.privacy.privacyplatform.auth;

import com.privacy.privacyplatform.auth.dto.*;
import com.privacy.privacyplatform.auth.entity.EmailVerification;
import com.privacy.privacyplatform.auth.repository.EmailVerificationRepository;
import com.privacy.privacyplatform.auth.service.EmailService;
import com.privacy.privacyplatform.security.JwtService;
import com.privacy.privacyplatform.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    /**
     * 1. 이메일 인증 코드 발송
     */
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        verificationRepository.deleteByEmail(email);

        String code = generateVerificationCode();

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verified(false)
                .build();

        verificationRepository.save(verification);
        emailService.sendVerificationCode(email, code);

        log.info("📧 인증 코드 발송: email={}", email);
    }

    /**
     * 2. 이메일 인증 코드 확인
     */
    public void verifyEmail(String email, String code) {
        EmailVerification verification = verificationRepository
                .findByEmailAndVerificationCodeAndVerifiedFalse(email, code)
                .orElseThrow(() -> new RuntimeException("인증 코드가 일치하지 않습니다."));

        if (verification.isExpired()) {
            throw new RuntimeException("인증 코드가 만료되었습니다.");
        }

        verification.setVerified(true);
        verificationRepository.save(verification);

        log.info("✅ 이메일 인증 완료: {}", email);
    }

    /**
     * 3. 회원가입
     */
    public AuthResponse register(RegisterRequest request) {
        // 인증 확인
        EmailVerification verification = verificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 인증이 필요합니다."));

        if (!verification.getVerified()) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }

        if (verification.isExpired()) {
            throw new RuntimeException("인증이 만료되었습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .emailVerified(true)
                .isActive(true)
                .build();

        userRepository.save(user);

        // JWT 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // RefreshToken 저장
        saveRefreshToken(user, refreshToken);

        log.info("✅ 회원가입 완료: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    /**
     * 4. 로그인 (간소화)
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다."));

        // ✅ 비밀번호 확인만
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        // ✅ 활성 계정 확인
        if (!user.getIsActive()) {
            throw new RuntimeException("비활성화된 계정입니다.");
        }

        // ❌ 삭제: 로그인 시도 체크
        // ❌ 삭제: 계정 잠금 체크
        // ❌ 삭제: IP 저장

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // JWT 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // RefreshToken 저장
        saveRefreshToken(user, refreshToken);

        log.info("✅ 로그인 성공: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    /**
     * 5. 토큰 갱신
     */
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 Refresh Token입니다."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("만료된 Refresh Token입니다.");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // 기존 토큰 삭제 후 새로운 토큰 저장
        refreshTokenRepository.delete(refreshToken);
        saveRefreshToken(user, newRefreshToken);

        log.info("🔄 토큰 갱신 완료: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    /**
     * 6. 로그아웃
     */
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
        log.info("👋 로그아웃 완료");
    }

    /**
     * RefreshToken 저장
     */
    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 인증 코드 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}