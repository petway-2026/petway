package com.petway.user.service;

import com.petway.user.dto.SignupRequest;
import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        User user = new User(
                request.email(),
                request.nickname()
        );

        userRepository.save(user);

        String passwordHash =
                passwordEncoder.encode(request.password());

        UserAuth userAuth = new UserAuth(
                user,
                AuthType.LOCAL,
                passwordHash
        );

        userAuthRepository.save(userAuth);

        return user.getId();
    }
}