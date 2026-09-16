/* 회원의 공개 프로필과 상태·권한을 응답으로 변환하며 비밀번호 정보는 포함하지 않는다. */
package com.petway.user.dto;

import com.petway.user.entity.User;
import com.petway.user.enums.UserRole;

public record UserResponse(

        Long userId,
        String email,
        String nickname,
        UserRole userRole

) {

    public static UserResponse from(
            User user
    ) {

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserRole()
        );
    }
}