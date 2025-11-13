package com.privacy.privacyplatform.auth.oauth2;

import com.privacy.privacyplatform.user.User;
import com.privacy.privacyplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        log.info("🔐 카카오 로그인 시도");
        log.info("📋 Attributes: {}", oAuth2User.getAttributes());

        KakaoOAuth2UserInfo kakaoUser = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());

        // 이메일 처리 (없으면 임시 이메일)
        String email = kakaoUser.getEmail();
        if (email == null || email.isEmpty()) {
            email = "kakao_" + kakaoUser.getId() + "@temp.privacy-platform.com";
            log.info("📧 이메일 미제공 - 임시 이메일 생성: {}", email);
        }

        // 닉네임 처리 (없으면 기본 닉네임)
        String nickname = kakaoUser.getNickname();
        if (nickname == null || nickname.isEmpty()) {
            String idPrefix = kakaoUser.getId();
            if (idPrefix.length() > 6) {
                idPrefix = idPrefix.substring(0, 6);
            }
            nickname = "카카오사용자" + idPrefix;
            log.info("👤 닉네임 미제공 - 기본 닉네임 생성: {}", nickname);
        }

        // 사용자 조회 또는 생성
        final String finalEmail = email;
        final String finalNickname = nickname;

        User user = userRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(UUID.randomUUID().toString())
                            .email(finalEmail)
                            .username(finalNickname)
                            .password("") // OAuth 사용자는 비밀번호 없음
                            .profileImageUrl(kakaoUser.getProfileImage())
                            .emailVerified(true) // 카카오 인증으로 간주
                            .isActive(true)
                            .build();

                    log.info("🆕 새 카카오 사용자 생성: email={}, nickname={}",
                            finalEmail, finalNickname);
                    return userRepository.save(newUser);
                });

        log.info("✅ 카카오 로그인 성공: email={}", finalEmail);

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}