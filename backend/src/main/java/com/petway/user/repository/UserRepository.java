/* 회원 이메일을 기준으로 회원 조회 및 가입 중복 확인을 수행한다. */
package com.petway.user.repository;

import com.petway.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}