/* 기존 계정 확인을 기다리는 카카오 연동 정보를 담는 임시 값이다. */
package com.petway.global.security.oauth;

import java.io.Serializable;

public record PendingKakaoLink(
        String email,
        String providerUserId
) implements Serializable {
}
