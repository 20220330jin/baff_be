package com.sa.baff.util;

public enum PieceTransactionType {
    DEPOSIT,        // 지급 (관리자/보상)
    BET_DEDUCT,     // 내기 차감 (배틀 시작 시)
    BET_WIN,        // 내기 승리 (상대 조각 획득)
    BET_REFUND      // 내기 환불 (무승부/취소)
}
