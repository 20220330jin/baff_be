package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    public ResponseEntity<?> userLogout(HttpServletResponse response) {
        String cookieHeader = String.format("accessToken=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None; Domain=%s", "baff-be.onrender.com");
        response.setHeader("Set-Cookie", cookieHeader);
        return ResponseEntity.ok("Logged out successfully");
    }
}
