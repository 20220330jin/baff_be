package com.sa.baff.service;

import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;

import java.util.List;

/**
 *
 * @author hjkim
 */
public interface UserService {
    UserDto getUserInfo(String userId);

    /**
     * 유저 대시보드 유저리스트 조회 서비스
     * @return
     */
    List<UserBDto.getUserList> getUserList();
}
