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
     * JWT 생성
     * @param userId
     * @return
     */
    public String create(String userId){
        Date expireDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        String jwt = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setSubject(userId).setIssuedAt(new Date()).setExpiration(expireDate)
                .compact();

        return jwt;
    }

    /**
     * JWT 검증
     */
    public String validate(String jwt){
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        String subject = null;

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
            subject = claims.getSubject();
        }catch (Exception exception){
            exception.printStackTrace();

            return null;
        }
        return subject;
    }
}
