package com.sa.baff.repository;

import com.sa.baff.domain.LinkToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LinkTokenRepository extends CrudRepository<LinkToken, Long> {
    Optional<LinkToken> findByToken(String token);
    Optional<LinkToken> findByIdempotencyKey(String idempotencyKey);

    /**
     * confirmLink 전용 비관적 락.
     * S3-15 P1-1 CP2 Round 2 반영: 동시 confirm 요청을 토큰 단위로 직렬화하여
     * READ_COMMITTED 환경에서도 idempotencyResponse 캐시 경로가 안정적으로 히트되도록 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM LinkToken t WHERE t.token = :token")
    Optional<LinkToken> findByTokenForUpdate(@Param("token") String token);
}
