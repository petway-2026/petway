/* 사용자를 확인하고 같은 대상이 이미 있으면 삭제, 없으면 저장하는 토글을 수행한다. */
package com.petway.favorite.service;
import com.petway.favorite.dto.*;
import com.petway.favorite.entity.Favorite;
import com.petway.favorite.repository.FavoriteRepository;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favorites; private final UserRepository users;
    private User user(String email) { return users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); }
    @Transactional(readOnly = true) public List<FavoriteResponse> list(String email) { return favorites.findByUserIdOrderByCreatedAtDesc(user(email).getId()).stream().map(FavoriteResponse::from).toList(); }
    @Transactional public FavoriteResponse toggle(String email, FavoriteRequest r) {
        User u = user(email); var old = favorites.findByUserIdAndTargetTypeAndTargetId(u.getId(), r.targetType(), r.targetId());
        if (old.isPresent()) { favorites.delete(old.get()); return null; }
        return FavoriteResponse.from(favorites.save(Favorite.create(u, r.targetType(), r.targetId(), r.title(), r.address(), r.thumbnailUrl())));
    }
    @Transactional public void delete(String email, String type, String id) { favorites.findByUserIdAndTargetTypeAndTargetId(user(email).getId(), type, id).ifPresent(favorites::delete); }
}
