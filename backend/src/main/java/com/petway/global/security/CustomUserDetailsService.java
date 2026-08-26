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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;


    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {


        /*
         * app_user 조회
         */
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "회원 정보를 찾을 수 없습니다."
                                )
                        );


        /*
         * 탈퇴 회원 체크
         */
        if (user.getUserStatus()
                != UserStatus.ACTIVE) {

            throw new UsernameNotFoundException(
                    "사용할 수 없는 회원입니다."
            );
        }


        /*
         * LOCAL 인증정보 조회
         */
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


        /*
         * Spring Security UserDetails 생성
         *
         * roles("USER")
         * roles("ADMIN")
         *
         * 형태로 Spring Security에 권한 등록
         *
         * 실제 내부 Authority:
         *
         * ROLE_USER
         * ROLE_ADMIN
         */
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(userAuth.getPasswordHash())
                .roles(user.getUserRole().name())
                .build();
    }
}