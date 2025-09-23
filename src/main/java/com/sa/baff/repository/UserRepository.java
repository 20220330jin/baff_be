package com.sa.baff.repository;

import com.sa.baff.domain.UserB;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<UserB, Long>, UserRepositoryCustom {
    // socialId를 통해 UserB 조회하는 메소드를 추가합니다.
    boolean existsBySocialId(String socialId);

    Optional<UserB> findUserIdBySocialId(String socialId);

    List<UserB> findAllByIdIn(List<Long> ids);

    Optional<UserB> findBySocialId(String socialId);
}
