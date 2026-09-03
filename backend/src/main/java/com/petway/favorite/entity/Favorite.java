package com.petway.favorite.entity;

import com.petway.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(name = "uk_favorite_user_target", columnNames = {"user_id", "target_type", "target_id"}), indexes = @Index(name = "idx_favorite_user_created", columnList = "user_id,created_at"))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "favorite_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "target_type", nullable = false, length = 20) private String targetType;
    @Column(name = "target_id", nullable = false, length = 150) private String targetId;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 300) private String address;
    @Column(name = "thumbnail_url", length = 500) private String thumbnailUrl;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public static Favorite create(User user, String type, String id, String title, String address, String thumbnail) {
        Favorite f = new Favorite(); f.user = user; f.targetType = type; f.targetId = id;
        f.title = title; f.address = address; f.thumbnailUrl = thumbnail; f.createdAt = LocalDateTime.now(); return f;
    }
}
