package com.petway.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
public class SecurityConfig {


    /**
     * BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    /**
     * AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    /**
     * Security 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * React 개발 서버 CORS
                 */
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )


                /*
                 * 현재 REST API 개발 단계에서는 비활성화.
                 *
                 * 실제 운영 전에는 CSRF 정책을 다시 설정하는 것을 권장.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )


                /*
                 * URL 접근 권한
                 */
                .authorizeHttpRequests(auth ->
                        auth

                                /*
                                 * 로그인하지 않아도 접근 가능
                                 */
                                .requestMatchers(
                                        "/api/auth/signup",
                                        "/api/auth/login",
                                        "/oauth2/**",
                                        "/login/oauth2/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()


                                /*
                                 * 그 외 API는 로그인 필요
                                 */
                                .anyRequest()
                                .authenticated()
                )


                /*
                 * React가 로그인 화면을 담당
                 */
                .formLogin(form ->
                        form.disable()
                )


                /*
                 * HTTP Basic 인증 사용 안 함
                 */
                .httpBasic(basic ->
                        basic.disable()
                )


                /*
                 * Session 방식
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                );


        return http.build();
    }


    /**
     * CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        /*
         * React Vite 개발 서버
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174"
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
         * JSESSIONID 쿠키 허용
         */
        configuration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}
