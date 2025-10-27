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

        // 요청이 들어온 서버의 호스트 이름을 기준으로 환경을 구분합니다.
        String serverName = request.getServerName();
        System.out.println(serverName +"--------------------------------");
        System.out.println("Request Server Name: " + serverName);

        String redirectUrl;

        if ("localhost".equalsIgnoreCase(serverName)) {
            // --- 로컬 환경 ---
            System.out.println("Environment: LOCAL");
            createAndSetCookieForLocal(response, "accessToken", token);
            redirectUrl = "http://localhost:5173/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
        } else if ("baff-be-ckop.onrender.com".equalsIgnoreCase(serverName)) {
            System.out.println("Environment: dev");
            createAndSetCookieForDevelopment(response, "accessToken", token);
            redirectUrl = "https://baff-fe.vercel.app/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
        } else {
            // --- 배포 환경 (개발/운영) ---
            // 참고: 현재 구조에서는 요청의 호스트 이름만으로는 개발(vercel)과 운영(change-up.me) 프론트엔드를 구분할 수 없습니다.
            // 두 환경 모두 동일한 백엔드(baff-be-ckop.onrender.com)를 호출하기 때문입니다.
            // 따라서 배포 환경에서는 우선 운영 도메인으로 리디렉션하도록 처리합니다.
            System.out.println("Environment: DEPLOYED");
            createAndSetCookieForProduction(response, "accessToken", token);
            redirectUrl = "https://change-up.me/user/oauth-response/" + token + "/" + EXPIRATION_TIME_SECONDS;
        }

        response.sendRedirect(redirectUrl);
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
