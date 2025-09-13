package com.sa.baff.handler;

import com.sa.baff.domain.CustomOAuth2User;
import com.sa.baff.provider.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    private static final long EXPIRATION_TIME_SECONDS = TimeUnit.DAYS.toSeconds(7);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        System.out.println("=============Success Handler============");
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String userId = oAuth2User.getName();
        String token = jwtProvider.create(userId);

        // --- 현재 활성화된 코드: 배포 환경용 ---
        createAndSetCookieForProduction(response, "accessToken", token);
        String redirectUrl = "https://baff-fe.vercel.app/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
        response.sendRedirect(redirectUrl);


        // --- 로컬 환경에서 사용시 아래 코드로 교체 ---
//        createAndSetCookieForLocal(response, "accessToken", token);
//        String localRedirectUrl = "http://localhost:5173/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
//        response.sendRedirect(localRedirectUrl);

    }

    /**
     * 배포 환경을 위한 쿠키 생성 메소드
     */
    private void createAndSetCookieForProduction(HttpServletResponse response, String key, String value) {
        System.out.println("DEBUG: createAndSetCookieForProduction called for key: " + key); // 이 줄을 추가합니다.
        String cookieValue = String.format(
                "%s=%s; Path=/; Max-Age=%d; SameSite=None; Secure; HttpOnly=true; Domain=baff-be-ckop.onrender.com",
                key, value, EXPIRATION_TIME_SECONDS
        );
        System.out.println("COOKIE VALUE: " + cookieValue);
        response.addHeader("Set-Cookie", cookieValue);
    }

    /**
     * 로컬 환경을 위한 쿠키 생성 메소드 (주석 처리됨)
     */
    private void createAndSetCookieForLocal(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge((int) EXPIRATION_TIME_SECONDS);
        response.addCookie(cookie);
    }
}
