package com.sa.baff.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryType {
    IMPROVEMENT,
    QUESTION,
    BUG,
    ALL
}