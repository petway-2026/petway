package com.petway.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionSecurityConfiguration {
    public ProductionSecurityConfiguration(FrontendProperties frontend, ServerProperties server,
                                           @Value("${KAKAO_REDIRECT_URI}") String redirectUri) {
        if (frontend.getOrigins().stream().anyMatch(origin -> !origin.startsWith("https://"))) {
            throw new IllegalArgumentException("운영 FRONTEND_ORIGINS에는 HTTPS origin이 필요합니다.");
        }
        URI callback = URI.create(redirectUri);
        if (!"https".equals(callback.getScheme()) || callback.getHost() == null
                || callback.getUserInfo() != null || callback.getQuery() != null || callback.getFragment() != null
                || !"/login/oauth2/code/kakao".equals(callback.getPath())) {
            throw new IllegalArgumentException("KAKAO_REDIRECT_URI에는 운영 HTTPS 카카오 콜백 주소가 필요합니다.");
        }
        var cookie = server.getServlet().getSession().getCookie();
        if (!Boolean.TRUE.equals(cookie.getSecure()) || !Boolean.TRUE.equals(cookie.getHttpOnly())
                || cookie.getSameSite() == null || "STRICT".equals(cookie.getSameSite().name())) {
            throw new IllegalArgumentException("운영 세션 쿠키에는 Secure, HttpOnly 및 SameSite=Lax 또는 None이 필요합니다.");
        }
    }
}
