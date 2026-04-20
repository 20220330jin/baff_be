package com.sa.baff.model.dto;

import java.time.LocalDateTime;

/**
 * 계정 통합 API DTO (spec §4.1~§4.4).
 */
public class AccountLinkDto {

    // issue-token
    public record IssueTokenResponse(String linkToken, int expiresIn) {}

    // prepare
    public record PrepareRequest(String linkToken, String tossUserKey) {}
    public record PrepareResponse(boolean canLink, Diff diff, Warnings warnings, String reason) {}
    public record Diff(GramsDiff grams, int weightLogs, int battles, int activeGoals) {}
    public record GramsDiff(long added, long total) {}
    public record Warnings(String nicknameFromPrimary, boolean attendanceFromTossOnly) {}

    // confirm
    public record ConfirmRequest(String linkToken, String idempotencyKey, String tossUserKey) {}
    public record ConfirmResponse(boolean success, Long primaryUserId, LocalDateTime linkedAt) {}

    // error
    public record ErrorResponse(String reason) {}
}
