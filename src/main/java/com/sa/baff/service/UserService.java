package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

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

    /**
     * 유저 정보 조회 서비스
     * @param userId
     * @return
     */
    UserBDto.getUserInfo getUserInfoForProfile(Long userId);

    ResponseEntity<?> userLogout(HttpServletRequest request, HttpServletResponse response);

    void insertHeight(String socialId, Double height);

    UserB findOrCreateSocialUser(String socialId, String email, String name, String profileUrl, String provider);

    void withdrawal(String socialId);

    void editProfileImage(String socialId, UserVO.editProfileImage param);

    UserDto.editNicknameStatus editNickname(String socialId, UserVO.editNicknameRequest param);

    List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(String socialId);

    void insertUserFlag(String socialId, String userFlag);
}
