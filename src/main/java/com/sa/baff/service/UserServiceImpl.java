package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.TossVO;
import com.sa.baff.model.vo.UserVO;
import com.sa.baff.provider.JwtProvider;
import com.sa.baff.repository.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserFlagRepository userFlagRepository;
    private final NicknameGeneratorService nicknameGeneratorService;
    private final JwtProvider jwtProvider;

    @Autowired(required = false)
    @Qualifier("tossWebClient")
    private WebClient tossWebClient;

    @Value("${toss.decrypt.key:}")
    private String tossDecryptKey;

    @Value("${toss.decrypt.aad:}")
    private String tossAad;

    @Override
    public UserDto getUserInfo(String userId) {
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
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));
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

    // ========== Toss 인증 ==========

    @Override
    public String loginWithToss(TossVO.LoginRequest request, HttpServletRequest httpRequest) {
        if (tossWebClient == null) {
            throw new IllegalStateException("Toss 연동이 설정되지 않았습니다.");
        }

        // 1. authorizationCode → accessToken
        TokenResponse tokenResponse = getAccessToken(request.getAuthorizationCode(), request.getReferrer());

        // 2. accessToken → 암호화된 사용자 정보
        UserInfo encryptedUserInfo = getUserInfoFromToss(tokenResponse.getSuccess().getAccessToken());

        // 3. 복호화
        String decryptedName = decryptField(encryptedUserInfo.getName());
        String decryptedEmail = decryptField(encryptedUserInfo.getEmail());

        log.info("Toss login - userKey: {}, name: {}", encryptedUserInfo.getUserKey(), decryptedName);

        // 4. 사용자 조회 또는 생성
        String socialId = String.valueOf(encryptedUserInfo.getUserKey());
        UserB user = userRepository.findBySocialId(socialId)
                .map(existingUser -> {
                    if (existingUser.getDelYn() == 'Y') {
                        log.info("Reactivating unlinked Toss user: {}", socialId);
                        userRepository.reactivate(existingUser.getId());
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    String email = (decryptedEmail != null && !decryptedEmail.isBlank())
                            ? decryptedEmail
                            : "toss_" + socialId + "@toss.im";

                    String randomImageUrl = nicknameGeneratorService.getRandomProfileImageUrl();
                    UserB newUser = new UserB(email, null, randomImageUrl, socialId, "toss", "TOSS");
                    nicknameGeneratorService.generateUniqueNicknameAndSave(newUser);
                    log.info("New Toss user created: {}", socialId);
                    return newUser;
                });

        // 5. JWT 생성
        return jwtProvider.create(user.getSocialId());
    }

    @Override
    public void unlinkTossAccount(String userKey) {
        log.info("Toss unlink-callback 수신 - userKey: {}", userKey);
        userRepository.findBySocialId(userKey).ifPresent(user -> {
            userRepository.withdrawal(user.getId());
            log.info("Toss 연결 해제 처리 완료 - userKey: {}, userId: {}", userKey, user.getId());
        });
    }

    // ========== Toss 내부 메서드 ==========

    private TokenResponse getAccessToken(String authorizationCode, String referrer) {
        log.info("Toss token 요청 - authorizationCode: {}", authorizationCode);

        try {
            String rawResponse = tossWebClient.post()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/generate-token")
                    .bodyValue(new TokenGenerationRequest(authorizationCode, referrer))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Toss token 응답: {}", rawResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(rawResponse, TokenResponse.class);

        } catch (WebClientResponseException e) {
            log.error("Toss token API 에러: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Toss token API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Toss token 요청 중 에러: {}", e.getMessage(), e);
            throw new RuntimeException("Toss token generation failed", e);
        }
    }

    private UserInfo getUserInfoFromToss(String accessToken) {
        log.info("Toss 사용자 정보 요청");

        try {
            String rawResponse = tossWebClient.get()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/login-me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Toss 사용자 정보 응답: {}", rawResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            TossUserInfoResponse response = objectMapper.readValue(rawResponse, TossUserInfoResponse.class);

            if (response != null && response.getSuccess() != null) {
                return response.getSuccess();
            }
            throw new RuntimeException("Toss user info response is null");

        } catch (WebClientResponseException e) {
            log.error("Toss user info API 에러: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Toss user info API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Toss 사용자 정보 요청 중 에러: {}", e.getMessage(), e);
            throw new RuntimeException("Toss user info retrieval failed", e);
        }
    }

    private String decryptField(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }

        try {
            final int IV_LENGTH = 12;
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] keyBytes = Base64.getDecoder().decode(tossDecryptKey);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            GCMParameterSpec nonceSpec = new GCMParameterSpec(16 * Byte.SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, nonceSpec);
            cipher.updateAAD(tossAad.getBytes());

            byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
            return new String(decrypted);

        } catch (Exception e) {
            log.error("Toss 복호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt Toss user info", e);
        }
    }

    // ========== Toss 내부 DTO ==========

    @Getter
    @RequiredArgsConstructor
    private static class TokenGenerationRequest {
        private final String authorizationCode;
        private final String referrer;
    }

    @Getter
    private static class TokenResponse {
        private String resultType;
        private SuccessResponse success;
    }

    @Getter
    private static class SuccessResponse {
        private String accessToken;
        private String refreshToken;
        private String expiresIn;
        private String tokenType;
        private String scope;
    }

    @Getter
    private static class TossUserInfoResponse {
        private String resultType;
        private UserInfo success;
    }

    @Getter @Setter
    private static class UserInfo {
        private Long userKey;
        private String scope;
        private String[] agreedTerms;
        private String policy;
        private String certTxId;
        private String name;
        private String callingCode;
        private String phone;
        private String birthday;
        private String ci;
        private String di;
        private String gender;
        private String nationality;
        private String email;
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
