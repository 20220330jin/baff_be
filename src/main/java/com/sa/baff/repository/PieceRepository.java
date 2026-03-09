package com.sa.baff.repository;

import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    Optional<Piece> findByUser(UserB user);
}
