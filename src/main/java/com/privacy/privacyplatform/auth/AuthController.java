package com.privacy.privacyplatform.auth;

import com.privacy.privacyplatform.auth.dto.*;
import com.privacy.privacyplatform.security.JwtService;
import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * 1. 이메일 인증 코드 발송
     */
    @PostMapping("/send-code")
    public ResponseEntity<String> sendVerificationCode(@RequestBody EmailRequest request) {
        log.info("📧 인증 코드 발송 요청: {}", request.getEmail());
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증 코드가 발송되었습니다.");
    }

    /**
     * 2. 이메일 인증 코드 확인
     */
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequest request) {
        log.info("✅ 인증 코드 확인 요청: {}", request.getEmail());
        authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    /**
     * 3. 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("📝 회원가입 요청: email={}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("🔐 로그인 요청: email={}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("🔄 토큰 갱신 요청");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 6. 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        log.info("👋 로그아웃 요청");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    /**
     * 7. 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 8. Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service OK");
    }
}