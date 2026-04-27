package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.Gender;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.TossVO;
import com.sa.baff.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
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

    /** S6-30: 성별 입력. 최초 1회 PROFILE_BONUS_GENDER 지급. 반환: 지급된 그램 (0이면 이미 받음). */
    int insertGender(String socialId, Gender gender);

    /** S6-30: 생년월일 입력. 최초 1회 PROFILE_BONUS_BIRTHDATE 지급. 반환: 지급된 그램. */
    int insertBirthdate(String socialId, LocalDate birthdate);

    UserB findOrCreateSocialUser(String socialId, String email, String name, String profileUrl, String provider);

    void withdrawal(String socialId);

    void editProfileImage(String socialId, UserVO.editProfileImage param);

    UserDto.editNicknameStatus editNickname(String socialId, UserVO.editNicknameRequest param);

    List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(String socialId);

    void insertUserFlag(String socialId, UserVO.insertUserFlag userFlag);

    String loginWithToss(TossVO.LoginRequest request, HttpServletRequest httpRequest);

    void unlinkTossAccount(String userKey);

    /**
     * 닉네임으로 유저 검색 (배틀 초대용)
     */
    List<UserBDto.searchResult> searchUsersByNickname(String nickname, String socialId);
}
