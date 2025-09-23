package com.sa.baff.handler;

import com.sa.baff.domain.CustomOAuth2User;
import com.sa.baff.domain.LoginHistory;
import com.sa.baff.provider.JwtProvider;
import com.sa.baff.repository.LoginHistoryRepository;
import com.sa.baff.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private static final long EXPIRATION_TIME_SECONDS = TimeUnit.DAYS.toSeconds(7);

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        System.out.println("=============Success Handler============");
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String userId = oAuth2User.getName();
        String token = jwtProvider.create(userId);

        String userAgent = request.getHeader("User-Agent");
        saveLoginHistory(userId, userAgent);

        // --- 현재 활성화된 코드: 배포 환경용 ---
        createAndSetCookieForDevelopment(response, "accessToken", token);
        String redirectUrl = "https://baff-fe.vercel.app/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
        response.sendRedirect(redirectUrl);


        // --- 로컬 환경에서 사용시 아래 코드로 교체 ---
//        createAndSetCookieForLocal(response, "accessToken", token);
//        String localRedirectUrl = "http://localhost:5173/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
//        response.sendRedirect(localRedirectUrl);

    }

    private void createAndSetCookieForProduction(HttpServletResponse response, String key, String value) {
        System.out.println("DEBUG: createAndSetCookieForProduction called for key: " + key);

        // ResponseCookie 빌더를 사용하여 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from(key, value)
                .path("/")
                .maxAge(EXPIRATION_TIME_SECONDS)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax") // "None" 대신 "Lax" 사용 가능 (아래 설명 참조)
                .domain(".change-up.me") // 새로운 공통 상위 도메인
                .build();

        System.out.println("COOKIE VALUE: " + cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 테스트 배포 환경을 위한 쿠키 생성 메소드
     */
    private void createAndSetCookieForDevelopment(HttpServletResponse response, String key, String value) {
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

    /**
     * 배포환경 로그아웃 처리 메소드
     */
    private void deleteCookieForProduction(HttpServletResponse response, String cookieName) {
        String cookieValue = String.format(
                "%s=; Path=/; Max-Age=0; SameSite=None; Secure; HttpOnly=true; Domain=baff-be-ckop.onrender.com",
                cookieName
        );
        response.addHeader("Set-Cookie", cookieValue);
        System.out.println("HttpOnly cookie deleted: " + cookieValue);
    }

    /**
     * 로그인 기록 저장
     */
    private void saveLoginHistory(String userId, String userAgent) {
        userRepository.findBySocialId(userId).ifPresent(user -> {
            // UserAgent 파싱
//            UserAgentParserService.ParsedResult parsedResult = userAgentParserService.parse(userAgent);

            // 파싱된 결과로 LoginHistory 객체 생성
            LoginHistory loginHistory = LoginHistory.builder()
                    .user(user)
                    .rawUserAgent(userAgent)
                    .build();

            loginHistoryRepository.save(loginHistory);
        });
    }

}
