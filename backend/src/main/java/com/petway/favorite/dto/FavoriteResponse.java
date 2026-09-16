/* 즐겨찾기 엔티티에서 사용자 화면에 필요한 대상 정보와 저장 시각을 추출한다. */
package com.petway.favorite.dto;
import com.petway.favorite.entity.Favorite;
import java.time.LocalDateTime;
public record FavoriteResponse(Long id, String targetType, String targetId, String title, String address, String thumbnailUrl, LocalDateTime createdAt) {
    public static FavoriteResponse from(Favorite f) { return new FavoriteResponse(f.getId(), f.getTargetType(), f.getTargetId(), f.getTitle(), f.getAddress(), f.getThumbnailUrl(), f.getCreatedAt()); }
}
