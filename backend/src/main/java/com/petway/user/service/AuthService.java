/* 일반 인증, 기존 계정 확인 후 카카오 연동, 현재 사용자 조회를 조정한다. */
package com.petway.user.service;

import com.petway.global.security.oauth.CustomOAuth2UserService;
import com.petway.global.security.oauth.PendingKakaoLink;
import com.petway.global.security.oauth.PendingKakaoLinkStore;
import com.petway.user.dto.LoginRequest;
import com.petway.user.dto.UserResponse;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final PendingKakaoLinkStore pendingKakaoLinkStore;

    private final CustomOAuth2UserService customOAuth2UserService;


    /**
     * 일반 로그인
     */
    public Authentication login(
            LoginRequest request
    ) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                );


        return authenticationManager.authenticate(token);
    }

    @Transactional
    public Authentication confirmAndLinkKakao(
            LoginRequest request,
            HttpSession session
    ) {
        PendingKakaoLink pendingLink = pendingKakaoLinkStore
                .find(session)
                .orElseThrow(() -> new IllegalStateException(
                        "연결 대기 중인 카카오 계정이 없습니다."
                ));

        if (!pendingLink.email().equalsIgnoreCase(request.email())) {
            throw new IllegalArgumentException(
                    "카카오 계정과 동일한 이메일을 입력해 주세요."
            );
        }

        Authentication authentication = login(request);
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "회원 정보를 찾을 수 없습니다."
                ));

        customOAuth2UserService.linkKakaoAccount(
                user,
                pendingLink.providerUserId()
        );
        pendingKakaoLinkStore.clear(session);

        return authentication;
    }


    /**
     * 현재 로그인 사용자 조회
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(
            String email
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "회원 정보를 찾을 수 없습니다."
                                )
                        );


        return UserResponse.from(user);
    }
}
