/* CustomOAuth2UserService의 동작을 검증하는 테스트다. 검증 대상: 카카오 사용자 정보를 PetWay 회원·인증정보에 연결하며 기존 계정과의 연동 조건을 검사한다. */
package com.petway.global.security.oauth;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserAuthRepository userAuthRepository;
    @Mock PendingKakaoLinkStore pendingLinkStore;

    CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(
                userRepository,
                userAuthRepository,
                pendingLinkStore
        );
    }

    @Test
    void createsNewKakaoUser() {
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "101"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@petway.test"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OAuth2User result = service.processKakaoUser(
                attributes("101", "new@petway.test")
        );

        assertEquals("new@petway.test", result.getName());
        verify(userAuthRepository).save(any(UserAuth.class));
    }

    @Test
    void logsInAlreadyLinkedKakaoUser() {
        User user = User.createOAuthUser("linked@petway.test", "linked");
        UserAuth auth = UserAuth.createKakao(user, "102");
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "102"))
                .thenReturn(Optional.of(auth));

        OAuth2User result = service.processKakaoUser(
                attributes("102", "linked@petway.test")
        );

        assertEquals("linked@petway.test", result.getName());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void sameEmailLocalAccountRequiresExplicitLink() {
        User localUser = User.create("same@petway.test", "local");
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "103"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("same@petway.test"))
                .thenReturn(Optional.of(localUser));
        when(userAuthRepository.findByUserAndAuthTypeAndProvider(
                localUser, AuthType.KAKAO, "kakao"))
                .thenReturn(Optional.empty());
        when(userAuthRepository.existsByUserAndAuthType(localUser, AuthType.LOCAL))
                .thenReturn(true);

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.processKakaoUser(
                        attributes("103", "same@petway.test")
                )
        );

        assertEquals(CustomOAuth2UserService.LINK_REQUIRED,
                exception.getError().getErrorCode());
        verify(pendingLinkStore).save(new PendingKakaoLink("same@petway.test", "103"));
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void rejectsDifferentProviderIdForUserWithKakaoLink() {
        User user = User.create("collision@petway.test", "user");
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "new-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("collision@petway.test"))
                .thenReturn(Optional.of(user));
        when(userAuthRepository.findByUserAndAuthTypeAndProvider(
                user, AuthType.KAKAO, "kakao"))
                .thenReturn(Optional.of(UserAuth.createKakao(user, "old-id")));

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.processKakaoUser(
                        attributes("new-id", "collision@petway.test")
                )
        );

        assertEquals(CustomOAuth2UserService.PROVIDER_CONFLICT,
                exception.getError().getErrorCode());
    }

    @Test
    void rejectsInactiveOAuthUser() {
        User user = User.createOAuthUser("inactive@petway.test", "inactive");
        user.withdraw();
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "104"))
                .thenReturn(Optional.of(UserAuth.createKakao(user, "104")));

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.processKakaoUser(
                        attributes("104", "inactive@petway.test")
                )
        );

        assertEquals(CustomOAuth2UserService.INACTIVE_USER,
                exception.getError().getErrorCode());
    }

    @Test
    void rejectsKakaoUserWithoutVerifiedEmail() {
        when(userAuthRepository.findByProviderAndProviderUserId("kakao", "105"))
                .thenReturn(Optional.empty());

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.processKakaoUser(Map.of(
                        "id", "105",
                        "kakao_account", Map.of(
                                "profile", Map.of("nickname", "tester")
                        )
                ))
        );

        assertEquals(CustomOAuth2UserService.EMAIL_REQUIRED,
                exception.getError().getErrorCode());
        verify(userRepository, never()).save(any());
    }

    private Map<String, Object> attributes(String id, String email) {
        return Map.of(
                "id", id,
                "kakao_account", Map.of(
                        "email", email,
                        "is_email_valid", true,
                        "is_email_verified", true,
                        "profile", Map.of("nickname", "tester")
                )
        );
    }
}
