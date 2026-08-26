package com.petway.user.service;

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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;


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