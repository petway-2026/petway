package com.petway.user.repository;

import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository
        extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByUserAndAuthType(
            User user,
            AuthType authType
    );

    Optional<UserAuth>
    findByProviderAndProviderUserId(
            String provider,
            String providerUserId
    );

    boolean existsByUserAndAuthType(
            User user,
            AuthType authType
    );
}