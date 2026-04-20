package com.sa.baff.service;

import com.sa.baff.common.GramConstants;
import com.sa.baff.domain.*;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.ExchangeStatus;
import com.sa.baff.util.PieceTransactionType;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExchangeServiceImpl implements ExchangeService {

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final ExchangeHistoryRepository exchangeHistoryRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final TossPromotionApiClient tossPromotionApiClient;

    @Value("${reward.dummy-mode:true}")
    private boolean dummyMode;

    @Value("${toss.promotion.exchange-code:}")
    private String exchangePromotionCode;

    @Override
    public RewardDto.exchangeResponse exchange(String socialId, Integer amount, Boolean adWatched) {
        // 금액 검증
        if (amount < GramConstants.EXCHANGE_MIN) {
            throw new IllegalArgumentException(
                    GramConstants.EXCHANGE_MIN + GramConstants.GRAM_NAME + " 이상부터 바꿀 수 있어요.");
        }
        if (amount > GramConstants.EXCHANGE_MAX) {
            throw new IllegalArgumentException(
                    "한 번에 " + GramConstants.EXCHANGE_MAX + GramConstants.GRAM_NAME + "까지 바꿀 수 있어요.");
        }

        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 비관적 락으로 잔액 조회
        Piece piece = pieceRepository.findByUserForUpdate(user)
                .orElseThrow(() -> new IllegalStateException("그램 정보가 없어요."));

        // 잔액 검증
        if (piece.getBalance() < amount) {
            throw new IllegalStateException("그램이 부족해요. 현재 " + piece.getBalance() + GramConstants.GRAM_NAME);
        }

        int tossAmount = amount * GramConstants.EXCHANGE_RATE;

        // PENDING 상태로 환전 기록 저장
        ExchangeHistory history = new ExchangeHistory(user.getId(), amount, tossAmount, adWatched);
        exchangeHistoryRepository.save(history);

        // 잔액 차감
        piece.deductForExchange((long) amount);
        pieceRepository.save(piece);

        // 차감 트랜잭션 기록
        PieceTransaction deductTx = PieceTransaction.builder()
                .user(user)
                .amount((long) amount)
                .type(PieceTransactionType.EXCHANGE_REQUEST)
                .build();
        pieceTransactionRepository.save(deductTx);

        // 토스 프로모션 API 호출
        try {
            if (!dummyMode) {
                String key = tossPromotionApiClient.grantReward(socialId, exchangePromotionCode, tossAmount);
                history.setPromotionKey(exchangePromotionCode);
                history.setTransactionId(key);
                log.info("환전 실제 지급: {}g→{}원 (userId={})", amount, tossAmount, user.getId());
            } else {
                log.info("더미 모드: 환전 {}g→{}원 (userId={})", amount, tossAmount, user.getId());
            }

            // 성공
            history.setStatus(ExchangeStatus.SUCCESS);
            exchangeHistoryRepository.save(history);

            // 리워드 히스토리에 차감 기록 (FE 내역에 표시)
            RewardHistory rewardHistory = new RewardHistory(
                    user.getId(), RewardType.EXCHANGE, -amount, RewardStatus.SUCCESS, null);
            rewardHistoryRepository.save(rewardHistory);

            PieceTransaction successTx = PieceTransaction.builder()
                    .user(user)
                    .amount((long) tossAmount)
                    .type(PieceTransactionType.EXCHANGE_SUCCESS)
                    .build();
            pieceTransactionRepository.save(successTx);

            return RewardDto.exchangeResponse.builder()
                    .success(true)
                    .pointAmount(amount)
                    .tossAmount(tossAmount)
                    .remainingBalance(piece.getBalance())
                    .message(amount + GramConstants.GRAM_NAME + "을 토스포인트 " + tossAmount + "원으로 바꿨어요!")
                    .build();

        } catch (Exception e) {
            // 실패 → 잔액 복구
            history.setStatus(ExchangeStatus.FAILED);
            history.setErrorMessage(e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "알 수 없는 오류");
            exchangeHistoryRepository.save(history);

            piece.addBalance((long) amount);
            pieceRepository.save(piece);

            PieceTransaction refundTx = PieceTransaction.builder()
                    .user(user)
                    .amount((long) amount)
                    .type(PieceTransactionType.EXCHANGE_FAILED_REFUND)
                    .build();
            pieceTransactionRepository.save(refundTx);

            log.error("환전 실패 (잔액 복구): userId={}, amount={}, error={}",
                    user.getId(), amount, e.getMessage());

            throw new RuntimeException("토스포인트로 바꾸는 중 오류가 발생했어요.", e);
        }
    }
}
