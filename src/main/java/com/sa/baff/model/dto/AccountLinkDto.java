package com.sa.baff.model.dto;

import java.time.LocalDateTime;

/**
 * 계정 통합 API DTO (spec §4.1~§4.4).
 */
public class AccountLinkDto {

    // issue-token
    public record IssueTokenResponse(String linkToken, int expiresIn) {}

    // prepare (v3: Plan Review Round 1 P0 + Round 2 P0)
    //   - Round 1: FE는 raw tossUserKey 생성 불가. authorizationCode/referrer만 전달
    //   - Round 2: canLink=true 응답에 nonce 포함 (Secondary 소유/동의 증명). 차단 시 null.
    public record PrepareRequest(String linkToken, String authorizationCode, String referrer) {}
    public record PrepareResponse(boolean canLink, Diff diff, Warnings warnings, String reason, String nonce) {}
    public record Diff(GramsDiff grams, int weightLogs, int battles, int activeGoals) {}
    public record GramsDiff(long added, long total) {}
    public record Warnings(String nicknameFromPrimary, boolean attendanceFromTossOnly) {}

    // confirm (v3: tossUserKey 제거, nonce 필수)
    public record ConfirmRequest(String linkToken, String idempotencyKey, String nonce) {}
    public record ConfirmResponse(boolean success, Long primaryUserId, LocalDateTime linkedAt) {}

    // error
    public record ErrorResponse(String reason) {}
}
