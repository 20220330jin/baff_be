package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.UserVO;
import com.sa.baff.repository.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserFlagRepository userFlagRepository;

    @Override
    public UserDto getUserInfo(String userId) {
        // userId는 JWT의 subject로, UserEntity의 socialId와 매핑됩니다.
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(userId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserDto.from(user);
    }

    @Override
    public List<UserBDto.getUserList> getUserList() {

        Iterable<UserB> user = userRepository.findAllByOrderByRegDateTimeDesc();
        List<UserBDto.getUserList> userDtoList = new ArrayList<>();

        for (UserB userB : user) {
            UserBDto.getUserList userDto = new UserBDto.getUserList();
            userDto.setUserId(userB.getId());
            userDto.setNickname(userB.getNickname());
            userDto.setEmail(userB.getEmail());
            userDto.setUserProfileUrl(userB.getProfileImageUrl());
            userDto.setRegDateTime(userB.getRegDateTime());
            userDto.setRole(userB.getRole());
            userDto.setStatus(userB.getDelYn().equals('N') ? "ACTIVE" : "INACTIVE");
            userDto.setPlatform(userB.getPlatform());
            userDto.setProvider(userB.getProvider());

            userDtoList.add(userDto);
        }

        return userDtoList;
    }

    @Override
    public UserBDto.getUserInfo getUserInfoForProfile(Long userId) {
        return userRepository.getUserInfoForProfile(userId);
    }

    @Override
    public ResponseEntity<?> userLogout(HttpServletRequest request, HttpServletResponse response) {
        if ("https://www.change-up.me".equals(request.getHeader("Origin"))) {
            // 운영 환경의 경우
            ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "") // 값을 비움
                    .path("/")
                    .maxAge(0) // Max-Age를 0으로 설정하여 즉시 삭제
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax") // 운영 환경 로그인 시 사용했던 SameSite 속성
                    .domain(".change-up.me") // 운영 환경의 공통 상위 도메인
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
            System.out.println("=================LOGOUT (Production)");


        } else {
            // 로컬 or 개발환경인경우
            String dynamicDomain = determineCookieDomain(request); // 동적으로 도메인 결정

            String cookieHeader = String.format("accessToken=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None; Domain=%s", dynamicDomain);
            response.setHeader("Set-Cookie", cookieHeader);
            System.out.println("=================LOGOUT");
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public void insertHeight(String socialId, Double height) {
        // socialId를 사용하여 데이터베이스에서 유저를 찾음
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 찾은 유저의 height 필드를 업데이트
        user.setHeight(height);

        // 변경된 내용을 저장
        userRepository.save(user);
    }

    @Override
    public UserB findOrCreateSocialUser(String socialId, String email, String name, String profileUrl, String provider) {
        UserB userEntity = userRepository.findBySocialId(socialId).orElse(null);

        String platform = "ANDROID";

        if (userEntity == null) {
            userEntity = new UserB(email, name, profileUrl, socialId, provider, platform);
            System.out.println("=====New user registered: " + socialId);
            userRepository.save(userEntity);
        } else {
            System.out.println("=====Existing user logged in: " + socialId);
        }
        return userEntity;
    }

    @Override
    @Transactional
    public void withdrawal(String socialId) {
        UserB user = userRepository.findBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        userRepository.withdrawal(user.getId());
    }

    @Override
    @Transactional
    public void editProfileImage(String socialId, UserVO.editProfileImage param) {
        UserB user = userRepository.findBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        userRepository.editProfileImage(user.getId(), param);
    }

    @Override
    @Transactional
    public UserDto.editNicknameStatus editNickname(String socialId, UserVO.editNicknameRequest param) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userRepository.editNickname(user.getId(), param.getNickname());
    }

    @Override
    public List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userFlagRepository.getUserFlagForPopUp(user.getId());
    }

    @Override
    public void insertUserFlag(String socialId, UserVO.insertUserFlag userFlag) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserFlag userFlag1 = new UserFlag();

        userFlag1.setFlagKey(userFlag.getFlagKey());
        userFlag1.setUser(user);

        userFlagRepository.save(userFlag1);
    }

    private String determineCookieDomain(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && origin.contains("localhost")) {
            return "localhost"; // 로컬 환경에서는 localhost 도메인 사용
        }
        // 배포 환경에서는 이전에 확인했던 점(.) 포함 도메인으로 통일
        return ".baff-be-ckop.onrender.com";
    }
}
