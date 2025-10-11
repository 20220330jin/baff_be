package com.sa.baff.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminSyncStatus {
    PENDING("전송 대기"),
    SUCCESS("전송 성공"),
    FAILED("전송 실패");

    private final String description;
}
