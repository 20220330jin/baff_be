package com.sa.baff.repository;

import com.sa.baff.domain.PieceTransaction;
import com.sa.baff.domain.UserB;
import com.sa.baff.util.PieceTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PieceTransactionRepository extends JpaRepository<PieceTransaction, Long> {

    List<PieceTransaction> findAllByUserOrderByRegDateTimeDesc(UserB user);

    // S6-16 — 그램경제 스냅샷 (특정 타입 × 기간 합계, 나만그래 pieceEconomyResponse 참조)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PieceTransaction t " +
           "WHERE t.type IN :types AND t.regDateTime >= :from")
    long sumAmountByTypesFrom(@Param("types") List<PieceTransactionType> types,
                              @Param("from") LocalDateTime from);
}
