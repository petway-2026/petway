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