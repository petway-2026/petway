/* 카카오 로그인 성공 후 프론트엔드로 이동시키는 인증 성공 핸들러다. */
package com.petway.global.security.oauth;

import com.petway.global.config.FrontendProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final FrontendProperties frontendProperties;


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
                frontendProperties.primaryOrigin() + "/"
        );
    }
}
