package com.sa.baff.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${secret-key}")
    private String secretKey;

    /**
     * JWT 생성 (role 포함)
     */
    public String create(String userId, String role) {
        Date expireDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .compact();
    }

    /**
     * JWT 생성 (하위 호환 — role 미지정 시 USER)
     */
    public String create(String userId) {
        return create(userId, "USER");
    }

    /**
     * JWT 검증 — userId(subject) 반환
     */
    public String validate(String jwt) {
        Claims claims = getClaims(jwt);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * JWT에서 role 클레임 추출
     */
    public String getRole(String jwt) {
        Claims claims = getClaims(jwt);
        if (claims == null) return null;
        return claims.get("role", String.class);
    }

    /**
     * JWT Claims 파싱
     */
    private Claims getClaims(String jwt) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
