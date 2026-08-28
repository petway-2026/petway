package com.petway.global.security.oauth;

import java.io.Serializable;

public record PendingKakaoLink(
        String email,
        String providerUserId
) implements Serializable {
}
