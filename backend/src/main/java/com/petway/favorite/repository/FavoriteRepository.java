/* 사용자별 즐겨찾기 목록과 동일 대상의 기존 즐겨찾기를 조회한다. */
package com.petway.favorite.repository;
import com.petway.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);
}
