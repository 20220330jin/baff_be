package com.sa.baff.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 요청 진입~응답 송신 직전 시간 측정 (BE 내부 처리 시간 정확 측정용).
 *
 * 외부 access log의 responseTimeMS는 (요청 도달부터 응답 송신까지)지만 네트워크 도달/송신 지연이 포함될 수 있음.
 * 이 필터는 Spring filter chain 진입~빠져나가는 순수 처리 시간을 ms 단위로 로깅.
 *
 * Filter chain 가장 앞단에 위치 (Order = HIGHEST_PRECEDENCE).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        String method = req.getMethod();

        // 측정 대상은 토스 미니앱 핵심 경로만 — 로그 노이즈 최소화
        boolean track = uri.startsWith("/api/toss/") || uri.startsWith("/api/user/") || uri.startsWith("/api/reward/");

        long start = track ? System.currentTimeMillis() : 0L;
        try {
            chain.doFilter(request, response);
        } finally {
            if (track) {
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed >= 500) {
                    log.warn("[REQ-SLOW] {} {} status={} elapsed={}ms", method, uri, res.getStatus(), elapsed);
                } else {
                    log.info("[REQ] {} {} status={} elapsed={}ms", method, uri, res.getStatus(), elapsed);
                }
            }
        }
    }
}
