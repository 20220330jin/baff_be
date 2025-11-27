package com.sa.baff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sa.baff.domain.CustomOAuth2User;
import com.sa.baff.domain.UserB;
import com.sa.baff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final NicknameGeneratorService nicknameGeneratorService;

    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("==============loadUser");

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String oauthClientName = userRequest.getClientRegistration().getClientName();

        try {
            System.out.println(new ObjectMapper().writeValueAsString(oAuth2User.getAttributes()));

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        UserB userEntity = null;
        String userId = null;
        String userEmail = null;
        String platform = "WEB";

        if(oauthClientName.equals("kakao")){
            System.out.println("===============loadUser KAKAO");
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            userId = "kakao_" + oAuth2User.getAttributes().get("id");
            userEmail = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

            if (!userRepository.existsBySocialIdAndDelYn(userId, 'N')) {
                // 1. 랜덤 프로필 이미지 URL 선택
                String randomImageUrl = nicknameGeneratorService.getRandomProfileImageUrl();

                // 2. 닉네임은 null로 설정하여 UserEntity 객체 생성
                userEntity = new UserB(userEmail, null, randomImageUrl, userId, "kakao", platform);

                // 3. 닉네임 생성 및 DB 저장을 NicknameGeneratorService에 위임 (트랜잭션 적용)
                nicknameGeneratorService.generateUniqueNicknameAndSave(userEntity);
            }
        } else if(oauthClientName.equals("Google")){
            System.out.println("===============loadUser Google");
            Map<String, Object> googleInfo = oAuth2User.getAttributes();
            userId = googleInfo.get("sub").toString();
            userEmail = googleInfo.get("email").toString();

            if (!userRepository.existsBySocialIdAndDelYn(userId, 'N')) {
                // 1. 랜덤 프로필 이미지 URL 선택
                String randomImageUrl = nicknameGeneratorService.getRandomProfileImageUrl();

                // 2. 닉네임은 null로 설정하여 UserEntity 객체 생성
                userEntity = new UserB(userEmail, null, randomImageUrl, userId, "google", platform);

                // 3. 닉네임 생성 및 DB 저장을 NicknameGeneratorService에 위임 (트랜잭션 적용)
                nicknameGeneratorService.generateUniqueNicknameAndSave(userEntity);
            }
        }

        return new CustomOAuth2User(userId);
    }
}
