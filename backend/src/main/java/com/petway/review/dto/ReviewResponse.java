/* 리뷰 내용, 평점, 작성자 닉네임과 작성·수정 시각을 화면에 반환한다. */
package com.petway.review.dto;
import com.petway.review.entity.Review; import java.time.LocalDateTime;
public record ReviewResponse(Long id,String targetType,String targetId,String nickname,int rating,String content,LocalDateTime createdAt,LocalDateTime updatedAt){ public static ReviewResponse from(Review r){return new ReviewResponse(r.getId(),r.getTargetType(),r.getTargetId(),r.getUser().getNickname(),r.getRating(),r.getContent(),r.getCreatedAt(),r.getUpdatedAt());}}
