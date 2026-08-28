package com.petway.global.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private static final String FRONT_URL =
            "http://localhost:5173";


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        /*
         * Spring Security가 OAuth2 로그인 성공 시
         * SecurityContext + HttpSession을 이미 생성한다.
         *
         * JWT 생성 필요 없음.
         */

        response.sendRedirect(
                FRONT_URL + "/"
        );
    }
}