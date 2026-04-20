package com.sa.baff.service.account;

import com.sa.baff.domain.UserB;

import java.util.Optional;

/**
 * socialId → UserB 조회 단일 진입점 (spec §3.6).
 *
 * 체인지업의 모든 socialId 기반 조회는 본 Resolver를 경유해야 한다.
 * 예외:
 *   - UserServiceImpl.loginWithToss() / unlinkTossAccount(): raw UserB 필요 (가입 생성 or unlink 경로)
 *   - AdminDashboard의 감사/사용자 목록: findRawBySocialId 또는 findById 허용
 *
 * resolve 우선순위:
 *   1) AccountLink.active → primary UserB 반환
 *   2) UserB.socialId 직접 매칭
 *   3) UserB.status=MERGED → primary_user_id 재조회
 *   4) status != ACTIVE 이면 empty
 */
public interface AccountLinkedUserResolver {
    /**
     * 일반 API 용도: socialId로 현재 활성 Primary UserB 반환.
     * MERGED/WITHDRAWN/INACTIVE는 결과에서 제외.
     */
    Optional<UserB> resolveActiveUserBySocialId(String socialId);

    /** 가입 중복 체크용: 병합/탈퇴/활성 모두 포함한 존재 여부. */
    boolean existsBySocialId(String socialId);

    /** 감사/디버깅/어드민 사용자 목록 용도: 원본 UserB (status 필터 없음). */
    Optional<UserB> findRawBySocialId(String socialId);
}
