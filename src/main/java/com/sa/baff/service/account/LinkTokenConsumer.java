package com.sa.baff.service.account;

import com.sa.baff.domain.type.LinkTokenStatus;
import com.sa.baff.repository.LinkTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 커밋 후 토큰 소비 (spec §4.3 TransactionSynchronization).
 * REQUIRES_NEW: 병합 트랜잭션과 독립적으로 커밋.
 */
@Component
@RequiredArgsConstructor
public class LinkTokenConsumer {

    private final LinkTokenRepository linkTokenRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(Long tokenId) {
        linkTokenRepository.findById(tokenId).ifPresent(t -> {
            t.setStatus(LinkTokenStatus.CONSUMED);
            t.setConsumedAt(LocalDateTime.now());
        });
    }
}
