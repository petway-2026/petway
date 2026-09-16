/* 회원의 인증 수단과 외부 제공자 식별자로 인증정보를 조회한다. */
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

    Optional<UserAuth> findByUserAndAuthTypeAndProvider(
            User user,
            AuthType authType,
            String provider
    );
}
