/* 동반 가능, 조건부 가능, 확인 필요, 동반 불가의 네 가지 판정 상태를 정의한다. */
package com.petway.compatibility;

public enum CompatibilityStatus {

    AVAILABLE,
    CONDITIONAL,
    NEED_CONFIRMATION,
    UNAVAILABLE

}
