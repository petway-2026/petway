package com.petway.review.dto;
import com.petway.review.entity.Review; import java.time.LocalDateTime;
public record ReviewResponse(Long id,String targetType,String targetId,String nickname,int rating,String content,LocalDateTime createdAt,LocalDateTime updatedAt){ public static ReviewResponse from(Review r){return new ReviewResponse(r.getId(),r.getTargetType(),r.getTargetId(),r.getUser().getNickname(),r.getRating(),r.getContent(),r.getCreatedAt(),r.getUpdatedAt());}}
