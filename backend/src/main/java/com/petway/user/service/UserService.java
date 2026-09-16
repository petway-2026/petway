/* 회원가입 시 회원과 LOCAL 인증정보를 함께 저장하고 이메일 기반 회원 조회를 제공한다. */
package com.petway.user.service;

import com.petway.user.dto.SignupRequest;
import com.petway.user.dto.UserResponse;
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


    /**
     * 일반 회원가입
     *
     * app_user
     * +
     * user_auth(LOCAL)
     * 두 테이블에 저장
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {

        /*
         * 1. 이메일 중복 검사
         */
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }


        /*
         * 2. app_user 생성
         *
         * User.create() 내부에서
         * userStatus = ACTIVE
         * userRole = USER
         * 로 설정
         */
        User user = User.create(
                request.email(),
                request.nickname()
        );

        User savedUser = userRepository.save(user);


        /*
         * 3. 비밀번호 암호화
         */
        String encodedPassword =
                passwordEncoder.encode(
                        request.password()
                );


        /*
         * 4. LOCAL 로그인 정보 생성
         */
        UserAuth userAuth =
                UserAuth.createLocal(
                        savedUser,
                        encodedPassword
                );

        userAuthRepository.save(userAuth);


        /*
         * 5. 회원정보 반환
         */
        return UserResponse.from(savedUser);
    }


    /**
     * 이메일로 회원 조회
     *
     * Authentication#getName() 값이
     * LOCAL / KAKAO 모두 이메일이 되도록
     * 맞춰놓았기 때문에 공통 사용 가능
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }


    /**
     * 현재 로그인한 회원 정보 조회
     *
     * GET /api/auth/me 에서 사용
     */
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String email) {

        User user =
                getUserByEmail(email);

        return UserResponse.from(user);
    }


    /**
     * LOCAL 인증 존재 여부
     *
     * 추후 계정 연동 여부 등을 확인할 때 사용 가능
     */
    @Transactional(readOnly = true)
    public boolean hasLocalAuth(String email) {

        User user =
                getUserByEmail(email);

        return userAuthRepository
                .existsByUserAndAuthType(
                        user,
                        AuthType.LOCAL
                );
    }


    /**
     * KAKAO 인증 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasKakaoAuth(String email) {

        User user =
                getUserByEmail(email);

        return userAuthRepository
                .existsByUserAndAuthType(
                        user,
                        AuthType.KAKAO
                );
    }
}