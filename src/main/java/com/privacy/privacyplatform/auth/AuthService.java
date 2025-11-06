package com.privacy.privacyplatform.auth;

import com.privacy.privacyplatform.auth.dto.AuthResponse;
import com.privacy.privacyplatform.auth.dto.LoginRequest;
import com.privacy.privacyplatform.auth.dto.RegisterRequest;
import com.privacy.privacyplatform.auth.dto.UserResponse;
import com.privacy.privacyplatform.security.JwtService;
import com.privacy.privacyplatform.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordValidator passwordValidator;
    private final RateLimitService rateLimitService;

    // 회원가입
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("📝 회원가입 시도: email={}", request.getEmail());

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        // 비밀번호 검증
        passwordValidator.validate(request.getPassword());

        // 사용자 생성
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .isActive(true)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .mustChangePassword(false)
                .build();

        userRepository.save(user);
        log.info("✅ 회원가입 완료: userId={}", user.getUserId());

        // 토큰 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Refresh Token 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .expiresIn(900L) // 15분 (초)
                .build();
    }

    // 로그인
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        log.info("🔐 로그인 시도: email={}", request.getEmail());

        // Rate Limiting 체크
        rateLimitService.checkLoginAttempts(request.getEmail(), ipAddress);

        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다"));

        // 계정 잠금 확인
        if (user.isAccountLocked()) {
            throw new RuntimeException("계정이 잠겨있습니다. " + user.getLockedUntil() + " 까지");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 실패 기록
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            rateLimitService.recordLoginAttempt(request.getEmail(), ipAddress, false);

            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다");
        }

        // 로그인 성공
        user.resetLoginAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        rateLimitService.recordLoginAttempt(request.getEmail(), ipAddress, true);

        // 기존 Refresh Token 무효화
        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        // 새 토큰 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Refresh Token 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("✅ 로그인 성공: userId={}", user.getUserId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .expiresIn(900L) // 15분
                .build();
    }

    // 내 정보 조회
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    // 로그아웃 (Refresh Token 무효화)
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            log.info("✅ 로그아웃: userId={}", token.getUser().getUserId());
        });
    }
}