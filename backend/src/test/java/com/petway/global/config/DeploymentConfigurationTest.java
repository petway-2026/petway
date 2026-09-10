package com.petway.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues("spring.profiles.active=prod",
                    "FRONTEND_ORIGINS=https://app.example.com");

    @Test
    void productionUsesSchemaValidationAndSecureSameSiteCookies() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(HibernateProperties.class).getDdlAuto()).isEqualTo("validate");
            var cookie = context.getBean(ServerProperties.class).getServlet().getSession().getCookie();
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.getHttpOnly()).isTrue();
            assertThat(cookie.getSameSite().name()).isEqualTo("LAX");
            assertThat(context.getBean(FrontendProperties.class).getOrigins())
                    .containsExactly("https://app.example.com");
        });
    }

    @Test
    void explicitCrossSiteCookieConfigurationKeepsSecure() {
        runner.withPropertyValues("SESSION_COOKIE_SAME_SITE=none").run(context -> {
            assertThat(context).hasNotFailed();
            var cookie = context.getBean(ServerProperties.class).getServlet().getSession().getCookie();
            assertThat(cookie.getSameSite().name()).isEqualTo("NONE");
            assertThat(cookie.getSecure()).isTrue();
        });
    }

    @Test
    void httpBootstrapUsesNonSecureSessionCookie() {
        runner.withPropertyValues(
                "FRONTEND_ORIGINS=http://petway2026.duckdns.org",
                "SESSION_COOKIE_SECURE=false"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ServerProperties.class).getServlet().getSession().getCookie().getSecure())
                    .isFalse();
        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"http,80", "https,443"})
    void forwardedOriginBuildsKakaoRedirectUri(String scheme, int port) throws Exception {
        ClientRegistration kakao = ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        ClientRegistrationRepository registrations = new InMemoryClientRegistrationRepository(kakao);
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");

        var request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        request.setScheme("http");
        request.setServerName("backend");
        request.setServerPort(8081);
        request.addHeader("X-Forwarded-Proto", scheme);
        request.addHeader("X-Forwarded-Host", "petway2026.duckdns.org");
        request.addHeader("X-Forwarded-Port", Integer.toString(port));
        var forwardedRequest = new AtomicReference<HttpServletRequest>();
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(),
                (wrappedRequest, response) -> forwardedRequest.set((HttpServletRequest) wrappedRequest));

        assertThat(resolver.resolve(forwardedRequest.get(), "kakao").getRedirectUri())
                .isEqualTo(scheme + "://petway2026.duckdns.org/login/oauth2/code/kakao");
    }

    @Test
    void productionRejectsEmptyOrigins() {
        runner.withPropertyValues("FRONTEND_ORIGINS=").run(context -> assertThat(context).hasFailed());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "FRONTEND_ORIGINS=http://app.example.com",
            "server.servlet.session.cookie.secure=false",
            "server.servlet.session.cookie.http-only=false",
            "SESSION_COOKIE_SAME_SITE=strict"
    })
    void rejectsUnsafeProductionOverrides(String property) {
        runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "*", "https://*.example.com", "https://example.com/path",
            "https://user:password@example.com", "https://example.com?x=1", "https://example.com#fragment"
    })
    void rejectsInvalidCorsOrigins(String origin) {
        assertThatThrownBy(() -> new FrontendProperties(origin)).isInstanceOf(IllegalArgumentException.class);
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ServerProperties.class, HibernateProperties.class})
    @Import({FrontendProperties.class, ProductionSecurityConfiguration.class})
    static class PropertiesConfig { }
}
