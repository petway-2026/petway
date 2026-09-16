/* 카카오 사용자 정보를 PetWay 회원·인증정보에 연결하며 기존 계정과의 연동 조건을 검사한다. */
package com.petway.global.security.oauth;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.enums.UserStatus;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String LINK_REQUIRED = "account_link_required";
    public static final String PROVIDER_CONFLICT = "provider_id_conflict";
    public static final String INACTIVE_USER = "inactive_user";
    public static final String EMAIL_REQUIRED = "verified_email_required";
    private static final String KAKAO = "kakao";

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PendingKakaoLinkStore pendingLinkStore;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        return processKakaoUser(super.loadUser(userRequest).getAttributes());
    }

    @Transactional
    public OAuth2User processKakaoUser(Map<String, Object> attributes) {
        String providerUserId = stringValue(attributes.get("id"));
        if (providerUserId == null || providerUserId.isBlank()) {
            throw oauthError("invalid_provider_response", "카카오 사용자 식별값이 없습니다.");
        }

        // 이미 연동된 카카오 식별자를 먼저 찾는다. 이메일 변경과 무관하게 기존 연결을 우선한다.
        UserAuth providerAuth = userAuthRepository
                .findByProviderAndProviderUserId(KAKAO, providerUserId)
                .orElse(null);
        if (providerAuth != null) {
            User linkedUser = providerAuth.getUser();
            requireActive(linkedUser);
            return principal(linkedUser, attributes);
        }

        Map<String, Object> account = mapValue(attributes.get("kakao_account"));
        String email = stringValue(account.get("email"));
        if (email == null
                || email.isBlank()
                || !booleanValue(account.get("is_email_valid"))
                || !booleanValue(account.get("is_email_verified"))) {
            throw oauthError(
                    EMAIL_REQUIRED,
                    "확인된 카카오 이메일 제공에 동의해야 로그인할 수 있습니다."
            );
        }

        Map<String, Object> profile = mapValue(account.get("profile"));
        String nickname = stringValue(profile.get("nickname"));
        if (nickname == null || nickname.isBlank()) {
            nickname = "PetWay 사용자";
        }

        // 이메일이 같다는 사실만으로 계정을 합치지 않는다. LOCAL 계정이면 기존 계정 확인을 거쳐 연동한다.
        User sameEmailUser = userRepository.findByEmail(email).orElse(null);
        if (sameEmailUser != null) {
            requireActive(sameEmailUser);

            if (userAuthRepository
                    .findByUserAndAuthTypeAndProvider(
                            sameEmailUser,
                            AuthType.KAKAO,
                            KAKAO
                    )
                    .isPresent()) {
                throw oauthError(
                        PROVIDER_CONFLICT,
                        "이미 다른 카카오 계정이 연결되어 있습니다."
                );
            }

            if (userAuthRepository
                    .existsByUserAndAuthType(sameEmailUser, AuthType.LOCAL)) {
                pendingLinkStore.save(new PendingKakaoLink(email, providerUserId));
                throw oauthError(
                        LINK_REQUIRED,
                        "기존 계정 확인 후 카카오 계정을 연결해야 합니다."
                );
            }

            throw oauthError(
                    PROVIDER_CONFLICT,
                    "동일 이메일 계정과 자동으로 연결할 수 없습니다."
            );
        }

        User newUser = userRepository.save(User.createOAuthUser(email, nickname));
        userAuthRepository.save(UserAuth.createKakao(newUser, providerUserId));
        return principal(newUser, attributes);
    }

    @Transactional
    public void linkKakaoAccount(User user, String providerUserId) {
        requireActive(user);

        if (userAuthRepository
                .findByProviderAndProviderUserId(KAKAO, providerUserId)
                .isPresent()) {
            throw new IllegalStateException("이미 연결된 카카오 계정입니다.");
        }

        if (userAuthRepository
                .findByUserAndAuthTypeAndProvider(user, AuthType.KAKAO, KAKAO)
                .isPresent()) {
            throw new IllegalStateException("이미 다른 카카오 계정이 연결되어 있습니다.");
        }

        userAuthRepository.save(UserAuth.createKakao(user, providerUserId));
    }

    private void requireActive(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw oauthError(INACTIVE_USER, "비활성화된 계정은 로그인할 수 없습니다.");
        }
    }

    private CustomOAuth2User principal(User user, Map<String, Object> attributes) {
        return new CustomOAuth2User(
                user.getEmail(),
                user.getUserRole(),
                attributes
        );
    }

    private OAuth2AuthenticationException oauthError(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value);
    }
}
