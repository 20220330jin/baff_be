package com.sa.baff.api;

import com.sa.baff.model.dto.PieceDto;
import com.sa.baff.service.PieceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piece")
@RequiredArgsConstructor
public class PieceRestController {

    private final PieceService pieceService;

    /**
     * 조각 잔액 조회
     */
    @GetMapping("/balance")
    public PieceDto.balanceResponse getBalance(@AuthenticationPrincipal String socialId) {
        return pieceService.getBalance(socialId);
    }

    /**
     * 조각 지급 (테스트/관리자용)
     */
    @PostMapping("/deposit")
    public void deposit(@AuthenticationPrincipal String socialId, @RequestBody Long amount) {
        pieceService.deposit(socialId, amount);
    }

    /**
     * 잔액 충분 여부 확인
     */
    @GetMapping("/check")
    public boolean checkBalance(@AuthenticationPrincipal String socialId, @RequestParam Long amount) {
        return pieceService.hasEnoughBalance(socialId, amount);
    }

    /**
     * 거래 내역 조회
     */
    @GetMapping("/history")
    public PieceDto.transactionHistoryResponse getHistory(@AuthenticationPrincipal String socialId) {
        return pieceService.getTransactionHistory(socialId);
    }
}
