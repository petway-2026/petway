package com.petway.global.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler
        implements AuthenticationFailureHandler {

    private static final String FRONT_URL =
            "http://localhost:5173";


    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String message =
                URLEncoder.encode(
                        "카카오 로그인에 실패했습니다.",
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                FRONT_URL +
                        "/login?oauthError=" +
                        message
        );
    }
}