package com.petway.user.entity;

import com.petway.user.enums.UserRole;
import com.petway.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(name = "user_id")
    private Long id;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
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
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /*
     * 기존 LOCAL 회원가입에서 사용
     */
    public static User create(
            String email,
            String nickname
    ) {
        User user = new User();

        user.email = email;
        user.nickname = nickname;
        user.userStatus = UserStatus.ACTIVE;
        user.userRole = UserRole.USER;

        return user;
    }


    /*
     * Kakao 최초 로그인에서 사용
     */
    public static User createOAuthUser(
            String email,
            String nickname
    ) {
        User user = new User();

        user.email = email;
        user.nickname = nickname;
        user.userStatus = UserStatus.ACTIVE;
        user.userRole = UserRole.USER;

        return user;
    }


    public void updateNickname(
            String nickname
    ) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.userStatus = UserStatus.WITHDRAWN;
    }


    @PrePersist
    protected void onCreate() {
        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now();
    }
}
