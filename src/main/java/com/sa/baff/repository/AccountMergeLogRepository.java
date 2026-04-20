package com.sa.baff.repository;

import com.sa.baff.domain.AccountMergeLog;
import org.springframework.data.repository.CrudRepository;

public interface AccountMergeLogRepository extends CrudRepository<AccountMergeLog, Long> {
    Iterable<AccountMergeLog> findByPrimaryUserId(Long primaryUserId);
}
