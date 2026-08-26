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
                        name = "uk_user_auth_provider_user",
                        columnNames = {
                                "provider",
                                "provider_user_id"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_auth_id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
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
            length = 20
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
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /**
     * LOCAL 회원 생성
     */
    public static UserAuth createLocal(
            User user,
            String passwordHash
    ) {

        UserAuth auth = new UserAuth();

        auth.user = user;
        auth.authType = AuthType.LOCAL;
        auth.passwordHash = passwordHash;

        auth.provider = null;
        auth.providerUserId = null;

        auth.createdAt = LocalDateTime.now();

        return auth;
    }
}