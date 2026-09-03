package com.petway.review.controller;
import com.petway.review.dto.*; import com.petway.review.service.ReviewService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/reviews") @RequiredArgsConstructor public class ReviewController{
 private final ReviewService service; @GetMapping public List<ReviewResponse> list(@RequestParam String targetType,@RequestParam String targetId){return service.list(targetType,targetId);}
 @PostMapping public ReviewResponse save(Authentication a,@Valid @RequestBody ReviewRequest r){return service.save(a.getName(),r);} @DeleteMapping("/{id}") public ResponseEntity<Void> delete(Authentication a,@PathVariable Long id){service.delete(a.getName(),id);return ResponseEntity.noContent().build();}
}
