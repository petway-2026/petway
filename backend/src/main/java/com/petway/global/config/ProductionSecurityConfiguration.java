/* 운영 환경의 origin 프로토콜과 세션 쿠키 Secure·HttpOnly·SameSite 설정의 일관성을 검사한다. */
package com.petway.global.config;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionSecurityConfiguration {
    public ProductionSecurityConfiguration(FrontendProperties frontend, ServerProperties server) {
        boolean httpsOrigins = frontend.getOrigins().stream().allMatch(origin -> origin.startsWith("https://"));
        boolean httpOrigins = frontend.getOrigins().stream().allMatch(origin -> origin.startsWith("http://"));
        var cookie = server.getServlet().getSession().getCookie();
        if ((!httpsOrigins && !httpOrigins) || httpsOrigins != Boolean.TRUE.equals(cookie.getSecure())) {
            throw new IllegalArgumentException("FRONTEND_ORIGINS scheme과 세션 cookie Secure 설정이 일치해야 합니다.");
        }
        if (!Boolean.TRUE.equals(cookie.getHttpOnly())
                || cookie.getSameSite() == null || "STRICT".equals(cookie.getSameSite().name())) {
            throw new IllegalArgumentException("운영 세션 쿠키에는 HttpOnly 및 SameSite=Lax 또는 None이 필요합니다.");
        }
    }
}
