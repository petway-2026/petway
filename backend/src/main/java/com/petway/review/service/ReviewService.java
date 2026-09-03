package com.petway.review.service;
import com.petway.review.dto.*; import com.petway.review.entity.Review; import com.petway.review.repository.ReviewRepository; import com.petway.user.entity.User; import com.petway.user.repository.UserRepository; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service @RequiredArgsConstructor public class ReviewService{
 private final ReviewRepository reviews; private final UserRepository users; private User user(String e){return users.findByEmail(e).orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));}
 @Transactional(readOnly=true) public List<ReviewResponse> list(String t,String i){return reviews.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(t,i).stream().map(ReviewResponse::from).toList();}
 @Transactional public ReviewResponse save(String e,ReviewRequest r){User u=user(e); Review x=reviews.findByUserIdAndTargetTypeAndTargetId(u.getId(),r.targetType(),r.targetId()).orElse(null); if(x==null)x=reviews.save(Review.create(u,r.targetType(),r.targetId(),r.rating(),r.content()));else{x.update(r.rating(),r.content());} return ReviewResponse.from(x);}
 @Transactional public void delete(String e,Long id){Review x=reviews.findById(id).orElseThrow();if(!x.getUser().getEmail().equals(e))throw new IllegalArgumentException("본인 리뷰만 삭제할 수 있습니다.");reviews.delete(x);}
}
