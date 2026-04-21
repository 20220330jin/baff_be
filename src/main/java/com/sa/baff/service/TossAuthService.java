package com.sa.baff.service;

/**
 * 토스 mTLS 인증 / 사용자 정보 조회 전담 서비스.
 *
 * Plan v3 Task 1.5-2 — UserServiceImpl.loginWithToss() 내부의 authorizationCode 교환 로직을 추출.
 * prepare/confirm API도 이 서비스를 경유하여 tossUserKey를 도출하며, raw userKey는 이 경계 밖으로
 * 노출되지 않는다 (Plan Review Round 1 P0 해소 경로).
 */
public interface TossAuthService {

    /**
     * authorizationCode + referrer로 토스 partner API를 호출해 사용자 정보를 얻고,
     * TossSocialIdMapper를 적용한 저장 포맷(tossUserKey)을 반환한다.
     *
     * @throws IllegalStateException Toss API 실패 또는 복호화 실패 시
     */
    TossUserKeyResult resolveTossUserKey(String authorizationCode, String referrer);

    /**
     * 토스 사용자 정보 결과 (prepare/login 공통).
     *
     * @param tossUserKey TossSocialIdMapper 적용 후 저장 형식 (UserB.socialId와 동일 규칙)
     * @param rawUserKey  토스 원본 userKey (로그/디버그용, 저장 금지)
     * @param email       복호화된 이메일 (nullable)
     * @param name        복호화된 이름 (nullable)
     */
    record TossUserKeyResult(String tossUserKey, Long rawUserKey, String email, String name) {}
}
