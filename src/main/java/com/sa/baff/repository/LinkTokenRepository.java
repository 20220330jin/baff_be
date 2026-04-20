package com.sa.baff.repository;

import com.sa.baff.domain.LinkToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LinkTokenRepository extends CrudRepository<LinkToken, Long> {
    Optional<LinkToken> findByToken(String token);
    Optional<LinkToken> findByIdempotencyKey(String idempotencyKey);
}
