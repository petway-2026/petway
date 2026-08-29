package com.petway.global.security;

import com.petway.global.config.FrontendProperties;
import com.petway.global.security.oauth.CustomOAuth2UserService;
import com.petway.global.security.oauth.OAuth2FailureHandler;
import com.petway.global.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService
            customOAuth2UserService;

    private final OAuth2SuccessHandler
            oAuth2SuccessHandler;

    private final OAuth2FailureHandler
            oAuth2FailureHandler;

    private final FrontendProperties frontendProperties;

    private final ObjectProvider<ClientRegistrationRepository>
            clientRegistrationRepository;


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * 개발 단계
                 *
                 * Session 방식이라 최종 배포 전에는
                 * CSRF 전략을 다시 잡는 게 좋음.
                 */
                .csrf(
                        csrf ->
                                csrf.disable()
                )


                .cors(
                        cors ->
                                cors.configurationSource(
                                        corsConfigurationSource()
                                )
                )


                .authorizeHttpRequests(
                        auth -> auth

                                .requestMatchers(
                                        "/api/auth/signup",
                                        "/api/auth/login",
                                        "/api/auth/oauth/kakao/link",
                                        "/api/places/**",

                                        "/oauth2/**",
                                        "/login/oauth2/**",
                                        "/error",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/api/admin/**"
                                )
                                .hasRole("ADMIN")

                                .anyRequest()
                                .authenticated()
                )


                .exceptionHandling(
                        exception -> exception
                                .defaultAuthenticationEntryPointFor(
                                        new HttpStatusEntryPoint(
                                                HttpStatus.FORBIDDEN
                                        ),
                                        new AntPathRequestMatcher(
                                                "/api/**"
                                        )
                                )
                )


                /*
                 * REST API 방식이므로
                 * 기본 login page 미사용
                 */
                .formLogin(
                        form ->
                                form.disable()
                )


                .httpBasic(
                        basic ->
                                basic.disable()
                )


                /*
                 * LOCAL/KAKAO 모두 HttpSession 사용
                 */
                .sessionManagement(
                        session ->
                                session
                                        .sessionCreationPolicy(
                                                SessionCreationPolicy.IF_REQUIRED
                                        )
                );

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(
                    oauth -> oauth
                            .authorizationEndpoint(endpoint -> endpoint
                                    .authorizationRequestResolver(
                                            kakaoAuthorizationRequestResolver(
                                                    clientRegistrationRepository.getObject()
                                            )
                                    ))
                            .userInfoEndpoint(userInfo -> userInfo
                                    .userService(customOAuth2UserService))
                            .successHandler(oAuth2SuccessHandler)
                            .failureHandler(oAuth2FailureHandler)
            );
        }


        return http.build();
    }

    private OAuth2AuthorizationRequestResolver
    kakaoAuthorizationRequestResolver(
            ClientRegistrationRepository registrations
    ) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        registrations,
                        "/oauth2/authorization"
                );

        resolver.setAuthorizationRequestCustomizer(
                request -> request.additionalParameters(
                        parameters -> parameters.put("prompt", "login")
                )
        );

        return resolver;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                frontendProperties.getOrigins()
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        /*
         * JSESSIONID 전송
         */
        configuration.setAllowCredentials(
                true
        );


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
