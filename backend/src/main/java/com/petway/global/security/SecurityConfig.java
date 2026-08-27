package com.petway.global.security;

import com.petway.global.security.oauth.CustomOAuth2UserService;
import com.petway.global.security.oauth.OAuth2FailureHandler;
import com.petway.global.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                 * Kakao OAuth2
                 */
                .oauth2Login(
                        oauth -> oauth

                                .userInfoEndpoint(
                                        userInfo ->
                                                userInfo
                                                        .userService(
                                                                customOAuth2UserService
                                                        )
                                )

                                .successHandler(
                                        oAuth2SuccessHandler
                                )

                                .failureHandler(
                                        oAuth2FailureHandler
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


        return http.build();
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
                List.of(
                        "http://localhost:5173"
                )
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
