/* 서버 렌더링 방식의 회원가입·로그인 페이지 경로와 회원가입 요청을 연결한다. */
package com.petway.user.controller;

import com.petway.user.dto.SignupRequest;
import com.petway.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid SignupRequest request,
            BindingResult bindingResult,
            Model model
    ) {

        if (bindingResult.hasErrors()) {
            return "user/signup";
        }

        try {
            userService.signup(request);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/signup";
        }

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }
}