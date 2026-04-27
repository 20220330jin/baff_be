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

    void reactivate(Long userId);

    /**
     * withdrawal로 변형된 socialId/email을 원본으로 되돌리고 활성화.
     * 'withdrawalUser_{원본}_{uuid}' → '{원본}'로 substring 후 status=ACTIVE, delYn='N' 세팅.
     */
    void reactivateWithRestore(Long userId, String originalSocialId, String originalEmail);

    void editProfileImage(Long userId, UserVO.editProfileImage param);

    UserDto.editNicknameStatus editNickname(Long userId, String nickname);
}
