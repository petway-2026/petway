package com.petway.user.controller;

import com.petway.user.dto.LoginRequest;
import com.petway.user.dto.SignupRequest;
import com.petway.user.dto.UserResponse;

import com.petway.user.service.AuthService;
import com.petway.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;


    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid
            @RequestBody
            SignupRequest request
    ) {

        Long userId =
                userService.signup(request);


        return ResponseEntity.ok(
                Map.of(
                        "userId", userId,
                        "message", "회원가입이 완료되었습니다."
                )
        );
    }


    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid
            @RequestBody
            LoginRequest request,

            HttpServletRequest httpRequest
    ) {

        /*
         * Spring Security 인증 실행
         */
        Authentication authentication =
                authService.login(request);


        /*
         * SecurityContext 생성
         */
        SecurityContext securityContext =
                SecurityContextHolder
                        .createEmptyContext();


        securityContext.setAuthentication(
                authentication
        );


        /*
         * 현재 요청에도 인증정보 등록
         */
        SecurityContextHolder.setContext(
                securityContext
        );


        /*
         * Session 생성
         */
        HttpSession session =
                httpRequest.getSession(true);


        /*
         * 다음 요청에서도 로그인 상태가 유지되도록
         * SecurityContext를 Session에 저장
         */
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );


        UserResponse user =
                authService.getCurrentUser(
                        authentication.getName()
                );


        return ResponseEntity.ok(
                Map.of(
                        "message", "로그인 성공",
                        "user", user
                )
        );
    }


    /**
     * 현재 로그인 사용자
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }


        UserResponse user =
                authService.getCurrentUser(
                        authentication.getName()
                );


        return ResponseEntity.ok(user);
    }


    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request
    ) {

        HttpSession session =
                request.getSession(false);


        if (session != null) {

            session.invalidate();
        }


        SecurityContextHolder.clearContext();


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "로그아웃되었습니다."
                )
        );
    }
}