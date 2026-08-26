package com.petway.user.entity;

import com.petway.user.enums.UserRole;
import com.petway.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_app_user_email",
                        columnNames = "email"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;


    @Column(
            name = "email",
            nullable = false,
            length = 150
    )
    private String email;


    @Column(
            name = "nickname",
            nullable = false,
            length = 50
    )
    private String nickname;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "user_status",
            nullable = false,
            length = 20
    )
    private UserStatus userStatus;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "user_role",
            nullable = false,
            length = 20
    )
    private UserRole userRole;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    /**
     * 일반 회원가입
     *
     * 클라이언트에서는 Role을 전달받지 않는다.
     * 회원가입으로 생성되는 회원은 항상 USER 권한이다.
     */
    public User(
            String email,
            String nickname
    ) {

        this.email = email;
        this.nickname = nickname;

        this.userStatus = UserStatus.ACTIVE;

        /*
         * 회원가입 시에는 무조건 USER
         *
         * ADMIN 권한은 회원가입 API를 통해 생성하지 않는다.
         */
        this.userRole = UserRole.USER;

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();
    }
}