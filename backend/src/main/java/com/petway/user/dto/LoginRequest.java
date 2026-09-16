/* 일반 로그인·계정 확인에 사용할 이메일과 비밀번호 입력을 검증한다. */
package com.petway.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,


        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}