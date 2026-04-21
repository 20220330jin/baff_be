package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.TossVO;
import com.sa.baff.model.vo.UserVO;
import com.sa.baff.provider.JwtProvider;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final UserFlagRepository userFlagRepository;
    private final NicknameGeneratorService nicknameGeneratorService;
    private final JwtProvider jwtProvider;
    private final TossAuthService tossAuthService;

    @Override
    public UserDto getUserInfo(String userId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
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
            ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .domain(".change-up.me")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
            System.out.println("=================LOGOUT (Production)");

        } else {
            String dynamicDomain = determineCookieDomain(request);

            String cookieHeader = String.format("accessToken=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None; Domain=%s", dynamicDomain);
            response.setHeader("Set-Cookie", cookieHeader);
            System.out.println("=================LOGOUT");
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public void insertHeight(String socialId, Double height) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setHeight(height);
        userRepository.save(user);
    }

    @Override
    public UserB findOrCreateSocialUser(String socialId, String email, String name, String profileUrl, String provider) {
        UserB userEntity = userRepository.findBySocialId(socialId).orElse(null);

        String platform = "ANDROID";

        if (userEntity == null) {
            String randomImageUrl = nicknameGeneratorService.getRandomProfileImageUrl();
            userEntity = new UserB(email, null, randomImageUrl, socialId, provider, platform);
            nicknameGeneratorService.generateUniqueNicknameAndSave(userEntity);
            System.out.println("=====New user registered: " + socialId);
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
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.editProfileImage(user.getId(), param);
    }

    @Override
    @Transactional
    public UserDto.editNicknameStatus editNickname(String socialId, UserVO.editNicknameRequest param) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userRepository.editNickname(user.getId(), param.getNickname());
    }

    @Override
    public List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userFlagRepository.getUserFlagForPopUp(user.getId());
    }

    @Override
    public void insertUserFlag(String socialId, UserVO.insertUserFlag userFlag) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserFlag userFlag1 = new UserFlag();
        userFlag1.setFlagKey(userFlag.getFlagKey());
        userFlag1.setUser(user);
        userFlagRepository.save(userFlag1);
    }

    // ========== Toss 인증 ==========

    @Override
    public String loginWithToss(TossVO.LoginRequest request, HttpServletRequest httpRequest) {
        TossAuthService.TossUserKeyResult tossResult =
                tossAuthService.resolveTossUserKey(request.getAuthorizationCode(), request.getReferrer());

        String socialId = tossResult.tossUserKey();
        UserB user = userRepository.findBySocialId(socialId)
                .map(existingUser -> {
                    if (existingUser.getDelYn() == 'Y') {
                        log.info("Reactivating unlinked Toss user: {}", socialId);
                        userRepository.reactivate(existingUser.getId());
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    String decryptedEmail = tossResult.email();
                    String email = (decryptedEmail != null && !decryptedEmail.isBlank())
                            ? decryptedEmail
                            : "toss_" + socialId + "@toss.im";

                    String randomImageUrl = nicknameGeneratorService.getRandomProfileImageUrl();
                    UserB newUser = new UserB(email, null, randomImageUrl, socialId, "toss", "TOSS");
                    nicknameGeneratorService.generateUniqueNicknameAndSave(newUser);
                    log.info("New Toss user created: {}", socialId);
                    return newUser;
                });

        return jwtProvider.create(user.getSocialId(), user.getRole().name());
    }

    @Override
    public void unlinkTossAccount(String userKey) {
        log.info("Toss unlink-callback 수신 - userKey: {}", userKey);
        String storedSocialId = com.sa.baff.util.TossSocialIdMapper.toStoredSocialId(userKey);
        userRepository.findBySocialId(storedSocialId).ifPresent(user -> {
            userRepository.withdrawal(user.getId());
            log.info("Toss 연결 해제 처리 완료 - userKey: {}, userId: {}", userKey, user.getId());
        });
    }

    @Override
    public List<UserBDto.searchResult> searchUsersByNickname(String nickname, String socialId) {
        List<UserB> users = userRepository.findByNicknameContainingAndDelYn(nickname, 'N');
        List<UserBDto.searchResult> results = new ArrayList<>();
        for (UserB user : users) {
            // 자기 자신 제외
            if (user.getSocialId().equals(socialId)) continue;
            UserBDto.searchResult dto = new UserBDto.searchResult();
            dto.setUserId(user.getId());
            dto.setNickname(user.getNickname());
            dto.setProfileImageUrl(user.getProfileImageUrl());
            results.add(dto);
        }
        return results;
    }

    // ========== 기존 유틸 ==========

    private String determineCookieDomain(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && origin.contains("localhost")) {
            return "localhost";
        }
        return ".baff-be-ckop.onrender.com";
    }
}
