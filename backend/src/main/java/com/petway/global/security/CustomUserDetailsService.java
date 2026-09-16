/* 활성 회원의 LOCAL 인증정보를 조회해 Spring Security의 비밀번호 인증에 연결한다. */
package com.petway.global.security;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.enums.UserStatus;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository
            userRepository;

    private final UserAuthRepository
            userAuthRepository;


    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "회원을 찾을 수 없습니다."
                                        )
                        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("비활성화된 계정입니다.");
        }


        UserAuth userAuth =
                userAuthRepository
                        .findByUserAndAuthType(
                                user,
                                AuthType.LOCAL
                        )
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "LOCAL 로그인 정보를 찾을 수 없습니다."
                                        )
                        );


        return org.springframework
                .security
                .core
                .userdetails
                .User
                .withUsername(
                        user.getEmail()
                )
                .password(
                        userAuth.getPasswordHash()
                )
                .roles(
                        user.getUserRole()
                                .name()
                )
                .build();
    }
}
