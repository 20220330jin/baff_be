package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto getUserInfo(String userId) {
        // userId는 JWT의 subject로, UserEntity의 socialId와 매핑됩니다.
        UserB user = userRepository.findUserIdBySocialId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserDto.from(user);
    }

    @Override
    public List<UserBDto.getUserList> getUserList() {

        Iterable<UserB> user = userRepository.findAll();
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
        String dynamicDomain = determineCookieDomain(request); // 동적으로 도메인 결정

        String cookieHeader = String.format("accessToken=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None; Domain=%s", dynamicDomain);
        response.setHeader("Set-Cookie", cookieHeader);
        System.out.println("=================LOGOUT");
        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public void insertHeight(String socialId, Double height) {
        // socialId를 사용하여 데이터베이스에서 유저를 찾음
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 찾은 유저의 height 필드를 업데이트
        user.setHeight(height);

        // 변경된 내용을 저장
        userRepository.save(user);
    }

    @Override
    public UserB findOrCreateSocialUser(String socialId, String email, String name, String profileUrl, String provider) {
        UserB userEntity = userRepository.findBySocialId(socialId).orElse(null);

        String platform = "ANDROID";

        if(userEntity == null) {
            userEntity = new UserB(email, name, profileUrl, socialId, provider, platform);
            System.out.println("=====New user registered: " + socialId);
        } else {
            System.out.println("=====Existing user logged in: " + socialId);
        }
        return userEntity;
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
