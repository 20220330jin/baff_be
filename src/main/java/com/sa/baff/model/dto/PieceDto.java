package com.sa.baff.model.dto;

import com.sa.baff.util.PieceTransactionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PieceDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class balanceResponse {
        private Long balance;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class transactionInfo {
        private Long amount;
        private PieceTransactionType type;
        private String description;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class transactionHistoryResponse {
        private Long balance;
        private List<transactionInfo> transactions;
    }
}
