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
        String userProfileUrl = null;
        String userName = null;
        String platform = "WEB";

        if(oauthClientName.equals("kakao")){
            System.out.println("===============loadUser KAKAO");
            Map<String, Object> properties = (Map<String, Object>) oAuth2User.getAttributes().get("properties");
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            String userNickname = properties != null ? (String) properties.get("nickname") : null;
            userId = "kakao_" + oAuth2User.getAttributes().get("id");
            userEmail = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            userProfileUrl = properties != null ? (String) properties.get("profile_image") : null;
            
            if (!userRepository.existsBySocialId(userId)) {
                userEntity = new UserB(userEmail, userNickname, userProfileUrl, userId, "kakao", platform);
                userRepository.save(userEntity);
            }
        } else if(oauthClientName.equals("Google")){
            System.out.println("===============loadUser Google");
            Map<String, Object> googleInfo = oAuth2User.getAttributes();
            userId = googleInfo.get("sub").toString();
            userName = googleInfo.get("name").toString();
            userEmail = googleInfo.get("email").toString();
            userProfileUrl = googleInfo.get("picture").toString();

            if (!userRepository.existsBySocialId(userId)) {
                userEntity = new UserB(userEmail, userName, userProfileUrl, userId, "google", platform);
                userRepository.save(userEntity);
            }
        }

        return new CustomOAuth2User(userId);
    }
}
