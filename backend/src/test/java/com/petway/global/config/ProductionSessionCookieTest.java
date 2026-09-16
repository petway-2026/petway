/* 운영 세션 쿠키의 보안 속성과 프론트엔드 origin 설정의 일관성을 검증한다. */
package com.petway.global.config;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionSessionCookieTest {
    @Test
    void embeddedServerActuallyEmitsProductionCookieAttributes() throws Exception {
        try (var context = new SpringApplicationBuilder(CookieApp.class).run(
                "--spring.profiles.active=prod", "--server.port=0", "--SESSION_COOKIE_SAME_SITE=lax",
                "--spring.main.banner-mode=off")) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            var response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cookie-test")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().allValues("Set-Cookie"))
                    .anySatisfy(cookie -> assertThat(cookie).contains("JSESSIONID=", "Secure", "HttpOnly", "SameSite=Lax"));
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    }, excludeName = "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration")
    static class CookieApp {
        @Bean
        ServletRegistrationBean<HttpServlet> cookieServlet() {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) {
                    request.getSession(true).setAttribute("test", true);
                    response.setStatus(200);
                }
            }, "/cookie-test");
        }
    }
}
