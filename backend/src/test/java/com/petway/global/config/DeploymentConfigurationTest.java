package com.petway.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues("spring.profiles.active=prod",
                    "KAKAO_REDIRECT_URI=https://api.example.com/login/oauth2/code/kakao",
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
    void productionRejectsEmptyOrigins() {
        runner.withPropertyValues("FRONTEND_ORIGINS=").run(context -> assertThat(context).hasFailed());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "FRONTEND_ORIGINS=http://app.example.com",
            "KAKAO_REDIRECT_URI=http://api.example.com/login/oauth2/code/kakao",
            "KAKAO_REDIRECT_URI=https://api.example.com/wrong-path",
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
