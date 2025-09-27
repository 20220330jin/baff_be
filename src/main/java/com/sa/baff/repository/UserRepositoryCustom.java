package com.sa.baff.repository;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;

import java.util.List;

public interface UserRepositoryCustom {
    /**
     * 유저 정보 조회
     * @param userId
     * @return
     */
    UserBDto.getUserInfo getUserInfoForProfile(Long userId);

    void withdrawal(Long userId);
}
