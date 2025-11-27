package com.sa.baff.repository;

import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.UserVO;

public interface UserRepositoryCustom {
    /**
     * 유저 정보 조회
     * @param userId
     * @return
     */
    UserBDto.getUserInfo getUserInfoForProfile(Long userId);

    void withdrawal(Long userId);

    void editProfileImage(Long userId, UserVO.editProfileImage param);

    UserDto.editNicknameStatus editNickname(Long userId, String nickname);
}
