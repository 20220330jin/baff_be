package com.sa.baff.service;

import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.PieceDto;

public interface PieceService {

    /**
     * 유저 잔액 조회 (없으면 0으로 자동 생성)
     */
    PieceDto.balanceResponse getBalance(String socialId);

    /**
     * 조각 지급 (관리자/보상)
     */
    void deposit(String socialId, Long amount);

    /**
     * 배틀 내기 차감 (배틀 시작 시 양쪽 모두)
     */
    void deductForBet(UserB user, Long amount, BattleRoom battleRoom);

    /**
     * 배틀 승리 조각 지급 (승자에게 양쪽 베팅 합산)
     */
    void grantBetWin(UserB winner, Long amount, BattleRoom battleRoom);

    /**
     * 무승부 시 조각 환불
     */
    void refundBet(UserB user, Long amount, BattleRoom battleRoom);

    /**
     * 잔액 확인 (부족하면 false)
     */
    boolean hasEnoughBalance(String socialId, Long amount);

    /**
     * 거래 내역 조회
     */
    PieceDto.transactionHistoryResponse getTransactionHistory(String socialId);
}
