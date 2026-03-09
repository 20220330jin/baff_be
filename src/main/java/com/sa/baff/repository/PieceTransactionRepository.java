package com.sa.baff.repository;

import com.sa.baff.domain.PieceTransaction;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PieceTransactionRepository extends JpaRepository<PieceTransaction, Long> {

    List<PieceTransaction> findAllByUserOrderByRegDateTimeDesc(UserB user);
}
