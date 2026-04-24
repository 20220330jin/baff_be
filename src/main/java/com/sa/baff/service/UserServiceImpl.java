package com.sa.baff.service;

import com.sa.baff.domain.AccountLink;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.Gender;
import com.sa.baff.util.RewardType;

import java.time.LocalDate;
import java.util.Optional;
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
    // S3-15 P1-3: 탈퇴 시 AccountLink revoke + Secondary 연쇄 탈퇴 (spec §3.4, §6.4)
    private final AccountLinkRepository accountLinkRepository;
    // S6-15: 최초 height 입력 시 프로필 완성 보너스 지급
    private final RewardService rewardService;

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

        // S6-15 프로필 완성 보너스 (최초 1회, dedup/실패는 RewardService 내부에서 swallow)
        rewardService.claimProfileBonus(user.getId(), user, RewardType.PROFILE_BONUS);
    }

    @Override
    public void insertGender(String socialId, Gender gender) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setGender(gender);
        userRepository.save(user);

        // S6-30 프로필 완성 보너스 - 성별 (최초 1회, dedup/실패는 RewardService 내부에서 swallow)
        rewardService.claimProfileBonus(user.getId(), user, RewardType.PROFILE_BONUS_GENDER);
    }

    @Override
    public void insertBirthdate(String socialId, LocalDate birthdate) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setBirthdate(birthdate);
        userRepository.save(user);

        // S6-30 프로필 완성 보너스 - 생년월일 (최초 1회, dedup/실패는 RewardService 내부에서 swallow)
        rewardService.claimProfileBonus(user.getId(), user, RewardType.PROFILE_BONUS_BIRTHDATE);
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
        cascadeWithdrawal(user.getId());
    }

    /**
     * S3-15 P1-3 — Primary 탈퇴 시 관련 리소스 동시 정리 (spec §3.4, §6.4).
     *  1) Primary 본인 status=WITHDRAWN + delYn='Y'
     *  2) Primary 소유 활성 AccountLink revoke
     *  3) 병합된 Secondary(MERGED) 조회 → 연쇄 WITHDRAWN + Secondary 소유 AccountLink 방어 revoke
     *
     * CP2 Round 2 Finding 3: happy path는 Primary 1건이지만 데이터 오염/운영 복구 이력을 방어.
     */
    private void cascadeWithdrawal(Long primaryUserId) {
        userRepository.withdrawal(primaryUserId);
        accountLinkRepository.findByUserIdAndStatus(primaryUserId, AccountLinkStatus.ACTIVE)
                .ifPresent(AccountLink::revoke);
        userRepository.findByPrimaryUserId(primaryUserId)
                .ifPresent(secondary -> {
                    userRepository.withdrawal(secondary.getId());
                    accountLinkRepository.findByUserIdAndStatus(secondary.getId(), AccountLinkStatus.ACTIVE)
                            .ifPresent(AccountLink::revoke);
                });
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
    @Transactional
    public void unlinkTossAccount(String userKey) {
        log.info("Toss unlink-callback 수신 - userKey: {}", userKey);
        String storedSocialId = com.sa.baff.util.TossSocialIdMapper.toStoredSocialId(userKey);

        // S3-15 P1-3 CP2 Round 2 Finding 1: 병합 후 AccountLink는 userId=primary, providerUserId=secondary.socialId 구조.
        // unlink 콜백이 Secondary socialId로 들어오면 raw findBySocialId는 Secondary row만 반환 → Primary가 안 잡힘.
        // 먼저 AccountLink로 Primary userId를 찾고, 없을 때만 standalone Toss 유저 fallback.
        Optional<AccountLink> activeLink = accountLinkRepository
                .findByProviderAndProviderUserIdAndStatus(
                        "toss", storedSocialId, AccountLinkStatus.ACTIVE);
        if (activeLink.isPresent()) {
            Long primaryUserId = activeLink.get().getUserId();
            cascadeWithdrawal(primaryUserId);
            log.info("Toss 연결 해제 처리 완료 (병합 계정 Primary 경유) - userKey: {}, primaryUserId: {}", userKey, primaryUserId);
            return;
        }
        userRepository.findBySocialId(storedSocialId).ifPresent(user -> {
            cascadeWithdrawal(user.getId());
            log.info("Toss 연결 해제 처리 완료 (standalone) - userKey: {}, userId: {}", userKey, user.getId());
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
