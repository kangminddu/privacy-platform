package com.privacy.privacyplatform.security;

import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT 토큰 추출
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractEmail(jwt);

            // 이메일이 있고 아직 인증되지 않은 경우
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 사용자 조회
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.validateToken(jwt, user.getEmail())) {
                    // 인증 토큰 생성
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            new ArrayList<>() // 권한 목록 (나중에 추가 가능)
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("✅ JWT 인증 성공: {}", userEmail);
                }
            }

        } catch (Exception e) {
            log.error("❌ JWT 인증 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}