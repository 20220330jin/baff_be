package com.sa.baff.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    RECEIVED,
    IN_PROGRESS,
    ANSWERED,
    CLOSED,
    // 필터에 필요한 ALL
    ALL
}