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
}
