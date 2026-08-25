package com.petway.global.security;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );

        UserAuth userAuth =
                userAuthRepository
                        .findByUserAndAuthType(
                                user,
                                AuthType.LOCAL
                        )
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "일반 로그인 계정이 아닙니다."
                                )
                        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(userAuth.getPasswordHash())
                .roles("USER")
                .build();
    }
}