/* 카카오 속성과 PetWay 회원 식별정보를 Spring Security의 OAuth 사용자 계약으로 제공한다. */
package com.petway.global.security.oauth;

import com.petway.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User
        implements OAuth2User {

    private final String email;

    private final UserRole userRole;

    private final Map<String, Object> attributes;


    public CustomOAuth2User(
            String email,
            UserRole userRole,
            Map<String, Object> attributes
    ) {
        this.email = email;
        this.userRole = userRole;
        this.attributes = attributes;
    }


    @Override
    public Map<String, Object>
    getAttributes() {
        return attributes;
    }


    @Override
    public Collection<? extends GrantedAuthority>
    getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" +
                                userRole.name()
                )
        );
    }


    /*
     * PetWay에서는 Authentication#getName()
     * 결과를 이메일로 통일
     */
    @Override
    public String getName() {
        return email;
    }


    public String getEmail() {
        return email;
    }
}