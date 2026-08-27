package com.petway.global.security.oauth;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository
            userRepository;

    private final UserAuthRepository
            userAuthRepository;


    @Override
    @Transactional
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {

        OAuth2User oauth2User =
                super.loadUser(userRequest);

        Map<String, Object> attributes =
                oauth2User.getAttributes();


        /*
         * Kakao 응답
         *
         * {
         *   id: 12345,
         *   kakao_account: {
         *      email: "...",
         *      profile: {
         *          nickname: "..."
         *      }
         *   }
         * }
         */

        Object idObject =
                attributes.get("id");

        if (idObject == null) {
            throw new OAuth2AuthenticationException(
                    "카카오 회원 식별값이 없습니다."
            );
        }

        String providerUserId =
                String.valueOf(idObject);

        Map<String, Object> kakaoAccount =
                castMap(
                        attributes.get(
                                "kakao_account"
                        )
                );

        String email =
                getString(
                        kakaoAccount,
                        "email"
                );

        /*
         * Kakao 이메일 권한을 사용할 수 없는 경우
         * provider_user_id 기반 내부 이메일 생성
         */
        if (email == null || email.isBlank()) {
            email =
                    "kakao_" +
                            providerUserId +
                            "@petway.local";
        }


        Map<String, Object> profile =
                castMap(
                        kakaoAccount.get(
                                "profile"
                        )
                );

        String nickname =
                getString(
                        profile,
                        "nickname"
                );

        if (nickname == null || nickname.isBlank()) {
            nickname = "PetWay 사용자";
        }


        /*
         * 1. 이미 Kakao 계정으로 가입되어 있는지 확인
         */
        UserAuth existingKakaoAuth =
                userAuthRepository
                        .findByProviderAndProviderUserId(
                                "kakao",
                                providerUserId
                        )
                        .orElse(null);


        User user;

        if (existingKakaoAuth != null) {

            /*
             * 기존 Kakao 회원
             */
            user =
                    existingKakaoAuth.getUser();

        } else {

            /*
             * 2. 동일 이메일 LOCAL 회원이 있는지 확인
             *
             * 있다면 app_user를 새로 만들지 않고
             * 해당 회원에 KAKAO 인증수단 추가
             */
            user =
                    userRepository
                            .findByEmail(email)
                            .orElse(null);


            if (user == null) {

                /*
                 * 3. 완전 신규 회원
                 */
                user =
                        User.createOAuthUser(
                                email,
                                nickname
                        );

                user =
                        userRepository.save(
                                user
                        );
            }


            /*
             * 4. user_auth에 KAKAO 인증 추가
             */
            boolean kakaoAlreadyExists =
                    userAuthRepository
                            .existsByUserAndAuthType(
                                    user,
                                    AuthType.KAKAO
                            );

            if (!kakaoAlreadyExists) {

                UserAuth kakaoAuth =
                        UserAuth.createKakao(
                                user,
                                providerUserId
                        );

                userAuthRepository.save(
                        kakaoAuth
                );
            }
        }


        return new CustomOAuth2User(
                user.getEmail(),
                user.getUserRole(),
                attributes
        );
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(
            Object value
    ) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Map.of();
    }


    private String getString(
            Map<String, Object> map,
            String key
    ) {
        Object value =
                map.get(key);

        return value == null
                ? null
                : String.valueOf(value);
    }
}