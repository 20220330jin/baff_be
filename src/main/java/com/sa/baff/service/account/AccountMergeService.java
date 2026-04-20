package com.sa.baff.service.account;

/**
 * 계정 병합 서비스 (spec §4.3, §4.4).
 * confirmLink 오케스트레이터가 호출하는 단일 병합 트랜잭션.
 */
public interface AccountMergeService {
    /**
     * Primary ← Secondary 병합 수행.
     * @return primaryUserId (idempotent return)
     */
    Long merge(Long primaryUserId, Long secondaryUserId);
}
