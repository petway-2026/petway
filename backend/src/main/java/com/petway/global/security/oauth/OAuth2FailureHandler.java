/* 카카오 인증 실패를 프론트엔드가 표시할 수 있는 리다이렉트 흐름으로 연결한다. */
package com.petway.global.security.oauth;

import com.petway.global.config.FrontendProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler
        implements AuthenticationFailureHandler {

    private final FrontendProperties frontendProperties;


    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        if (exception instanceof OAuth2AuthenticationException oauthException
                && CustomOAuth2UserService.LINK_REQUIRED.equals(
                oauthException.getError().getErrorCode())) {
            response.sendRedirect(
                    frontendProperties.primaryOrigin()
                            + "/login?oauthLinkRequired=true"
            );
            return;
        }

        String message =
                URLEncoder.encode(
                        "카카오 로그인에 실패했습니다.",
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                frontendProperties.primaryOrigin() +
                        "/login?oauthError=" +
                        message
        );
    }
}
