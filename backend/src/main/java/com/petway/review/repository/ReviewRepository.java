package com.petway.review.repository;
import com.petway.review.entity.Review; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ReviewRepository extends JpaRepository<Review,Long>{ List<Review> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String t,String i); Optional<Review> findByUserIdAndTargetTypeAndTargetId(Long u,String t,String i); }
