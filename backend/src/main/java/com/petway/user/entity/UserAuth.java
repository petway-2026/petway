package com.petway.user.entity;

import com.petway.user.enums.AuthType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_auth_provider",
                        columnNames = {
                                "provider",
                                "provider_user_id"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(
        access = AccessLevel.PROTECTED
)
public class UserAuth {

    @Id
    @GeneratedValue(
            strategy =
                    GenerationType.IDENTITY
    )
    @Column(name = "user_auth_id")
    private Long id;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "auth_type",
            nullable = false,
            length = 20
    )
    private AuthType authType;


    @Column(
            name = "provider",
            length = 30
    )
    private String provider;


    @Column(
            name = "provider_user_id",
            length = 100
    )
    private String providerUserId;


    @Column(
            name = "password_hash",
            length = 255
    )
    private String passwordHash;


    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    /*
     * 기존 LOCAL 회원가입
     */
    public static UserAuth createLocal(
            User user,
            String passwordHash
    ) {
        UserAuth auth =
                new UserAuth();

        auth.user = user;
        auth.authType =
                AuthType.LOCAL;

        auth.provider = "local";
        auth.providerUserId = null;

        auth.passwordHash =
                passwordHash;

        return auth;
    }


    /*
     * Kakao 로그인
     */
    public static UserAuth createKakao(
            User user,
            String kakaoUserId
    ) {
        UserAuth auth =
                new UserAuth();

        auth.user = user;
        auth.authType =
                AuthType.KAKAO;

        auth.provider =
                "kakao";

        auth.providerUserId =
                kakaoUserId;

        auth.passwordHash =
                null;

        return auth;
    }


    @PrePersist
    protected void onCreate() {
        createdAt =
                LocalDateTime.now();
    }
}