package com.sa.baff.repository;

import com.sa.baff.domain.UserFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFlagRepository extends JpaRepository<UserFlag, Long>, UserFlagRepositoryCustom {
}
