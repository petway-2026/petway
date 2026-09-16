/* 대상별 리뷰 목록 및 동일 사용자가 작성한 기존 리뷰를 조회한다. */
package com.petway.review.repository;
import com.petway.review.entity.Review; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ReviewRepository extends JpaRepository<Review,Long>{ List<Review> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String t,String i); Optional<Review> findByUserIdAndTargetTypeAndTargetId(Long u,String t,String i); }
