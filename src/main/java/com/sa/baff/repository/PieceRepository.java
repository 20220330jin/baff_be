package com.sa.baff.repository;

import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    Optional<Piece> findByUser(UserB user);

    /** 비관적 락 (SELECT FOR UPDATE) -- 환전 동시성 제어용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Piece p WHERE p.user = :user")
    Optional<Piece> findByUserForUpdate(@Param("user") UserB user);

    // S6-16 — 그램경제 스냅샷 집계 (나만그래 pieceEconomyResponse 참조)
    @Query("SELECT COALESCE(SUM(p.balance), 0) FROM Piece p")
    long sumCirculating();

    @Query("SELECT COALESCE(SUM(p.totalEarned), 0) FROM Piece p")
    long sumTotalEarned();

    @Query("SELECT COALESCE(SUM(p.totalExchanged), 0) FROM Piece p")
    long sumTotalExchanged();

    @Query("SELECT COUNT(p) FROM Piece p WHERE p.balance > 0")
    long countHolders();
}
