package com.sa.baff.config;

import com.sa.baff.domain.FeatureAccessConfig;
import com.sa.baff.repository.FeatureAccessConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S6-2 기능 접근 제어 초기 데이터 시딩.
 * 테이블이 비어있을 때만 8건 기본값 삽입 (중복 실행 안전).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(100)
public class FeatureAccessConfigInitializer implements ApplicationRunner {

    private final FeatureAccessConfigRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("[FeatureAccessConfig] 기존 설정 존재 — 시딩 생략");
            return;
        }

        log.info("[FeatureAccessConfig] 초기 8건 시딩");

        repository.saveAll(List.of(
                new FeatureAccessConfig("AI_ANALYSIS", false, true, "AI 체중 분석 (Anthropic 호출)"),
                new FeatureAccessConfig("LEADERBOARD", false, true, "감량 리더보드 (S6-22)"),
                new FeatureAccessConfig("ACCOUNT_LINK_TOSS", false, true, "토스 계정 연결 (S3-10)"),
                new FeatureAccessConfig("RUNNING", true, true, "달리기 기록 프로토타입"),
                new FeatureAccessConfig("FASTING", true, true, "간헐적 단식 관리"),
                new FeatureAccessConfig("BATTLE_CREATE", true, true, "배틀 방 생성"),
                new FeatureAccessConfig("EXCHANGE", true, true, "토스포인트 환전"),
                new FeatureAccessConfig("REVIEW_WRITE", true, true, "후기 작성")
        ));
    }
}
