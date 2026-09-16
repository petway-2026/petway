/* 계정 연동 대기 정보를 세션에 보관하고 확인 과정에서 조회·정리한다. */
package com.petway.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PendingKakaoLinkStore {

    private static final String SESSION_KEY =
            PendingKakaoLinkStore.class.getName() + ".PENDING";

    private final HttpServletRequest request;

    public void save(PendingKakaoLink pendingLink) {
        request.getSession(true).setAttribute(SESSION_KEY, pendingLink);
    }

    public Optional<PendingKakaoLink> find(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                (PendingKakaoLink) session.getAttribute(SESSION_KEY)
        );
    }

    public void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }
}
