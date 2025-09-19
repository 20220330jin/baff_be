package com.sa.baff.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.GoogleMobileLoginRequestDto; // 사용자님의 DTO 임포트 경로
import com.sa.baff.provider.JwtProvider;
import com.sa.baff.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtProvider jwtProvider;

    // TODO: 이 값은 application.yml 등 설정 파일에서 @Value 어노테이션으로 주입받는 것이 좋습니다.
    private static final String GOOGLE_WEB_CLIENT_ID = "1068438948743-r2a9hcnpuc8uphj75e0msmqv6qqhgrel.apps.googleusercontent.com"; // 실제 웹 클라이언트 ID
    private final UserService userService;

    @Autowired
    public AuthController(JwtProvider jwtProvider, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @PostMapping("/google/mobile")
    public ResponseEntity<?> googleMobileLogin(@RequestBody GoogleMobileLoginRequestDto request) {
        try {
            // 1. Google-issued idToken 검증
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = new GsonFactory();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(GOOGLE_WEB_CLIENT_ID))
                .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            System.out.println("IDTOKEN:-----------------" + idToken);

            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleUserId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String profileUrl = (String) payload.get("picture");

            UserB user = userService.findOrCreateSocialUser(googleUserId, email, name, profileUrl, "google");

            // 2. 백엔드 사용자 처리 (찾거나 새로 생성)
            // TODO: 실제 서비스에서는 이메일, Google ID 등을 기반으로 사용자 정보를 조회하거나 새로 생성하는 로직이 필요합니다.
            String userId = googleUserId; // 백엔드에서 관리할 사용자 ID 형식 (예시)

            // 3. 백엔드-issued JWT 생성
            String backendJwt = jwtProvider.create(user.getSocialId());

            System.out.println("Google Mobile Login Success: " + email + ", " + name + ", " + userId);
            System.out.println("Generated Backend JWT: " + backendJwt);

            // 4. 응답 반환
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", backendJwt);
            // TODO: 실제 사용자 정보 객체를 반환하도록 수정
            responseBody.put("user", Map.of("email", email, "name", name, "socialId", user.getSocialId()));

            return ResponseEntity.ok(responseBody);

        } catch (GeneralSecurityException e) {
            System.err.println("Google Mobile Login Security Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google ID Token Security Error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Google Mobile Login IO Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Google ID Token IO Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Google Mobile Login Unexpected Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Google Mobile Login Failed: " + e.getMessage());
        }
    }
}