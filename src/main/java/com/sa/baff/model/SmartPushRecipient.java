package com.sa.baff.model;

/**
 * 스마트발송 수신자 (spec §3.2 — CP1 Round 2 P0 반영).
 *
 * 발송 키와 이력 키를 분리하는 이유:
 * - tossUserKey: 토스 API x-toss-user-key 헤더 값. 병합 Primary의 경우 AccountLink.providerUserId
 * - userId: SmartPushHistory 저장 및 일일 중복 방지 가드 기준. Primary UserB.id
 *
 * 카카오/구글 유저의 socialId를 토스 API에 잘못 전송하는 문제 방지.
 */
public record SmartPushRecipient(Long userId, String tossUserKey) {}
