package com.privacy.privacyplatform.auth.oauth2;

import com.privacy.privacyplatform.security.JwtService;
import com.privacy.privacyplatform.user.RefreshToken;
import com.privacy.privacyplatform.user.RefreshTokenRepository;
import com.privacy.privacyplatform.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // JWT 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // RefreshToken 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("🎉 카카오 인증 성공 - JWT 발급: email={}", user.getEmail());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", user.getUserId())
                .queryParam("email", user.getEmail())
                .queryParam("username", user.getUsername())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        log.info("🔀 리다이렉트: {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}