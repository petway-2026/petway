/* 사용자의 대상별 평점과 리뷰 내용, 작성·수정 시각을 저장한다. */
package com.petway.review.entity;
import com.petway.user.entity.User;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="review", uniqueConstraints=@UniqueConstraint(name="uk_review_user_target", columnNames={"user_id","target_type","target_id"}))
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="review_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="target_type",nullable=false,length=20) private String targetType; @Column(name="target_id",nullable=false,length=150) private String targetId;
 @Column(nullable=false) private int rating; @Column(nullable=false,length=100) private String content; @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at") private LocalDateTime updatedAt;
 public static Review create(User u,String t,String i,int r,String c){ Review x=new Review();x.user=u;x.targetType=t;x.targetId=i;x.rating=r;x.content=c;x.createdAt=LocalDateTime.now();return x; }
 public void update(int r,String c){rating=r;content=c;updatedAt=LocalDateTime.now();}
}
