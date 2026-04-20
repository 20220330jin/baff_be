package com.sa.baff.util;

/**
 * 토스 userKey ↔ UserB.socialId 매핑 (spec §3.5).
 *
 * 1차 구현: 현행 UserServiceImpl.loginWithToss()의 String.valueOf(...)와 동등한 pass-through.
 * 향후 암호화 저장 전환은 기존 유저 데이터 마이그레이션 + dual-read 기간이 필요한 별도 과업.
 * S3 범위에서는 저장 포맷 변경 없음.
 *
 * 4경로 단일 구현 공유 (spec §6.8 수용기준):
 *   - UserServiceImpl.loginWithToss()
 *   - UserServiceImpl.unlinkTossAccount()
 *   - AccountLinkServiceImpl.prepareLink()
 *   - AccountLinkServiceImpl.confirmLink()
 */
public final class TossSocialIdMapper {
    private TossSocialIdMapper() {}

    public static String toStoredSocialId(Object rawTossUserKey) {
        return String.valueOf(rawTossUserKey);
    }
}
