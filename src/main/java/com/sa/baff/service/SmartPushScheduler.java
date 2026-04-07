package com.sa.baff.service;

import com.sa.baff.util.SmartPushType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartPushScheduler {

    private final SmartPushService smartPushService;

    /** 매일 오전 10시 - 환전 안 한 사용자 알림 */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void executeExchangeReminder() {
        log.info("스마트발송 스케줄 실행: EXCHANGE_REMINDER");
        smartPushService.executePush(SmartPushType.EXCHANGE_REMINDER);
    }

    /** 매일 오후 8시 - 출석 안 한 사용자 알림 */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    public void executeAttendanceReminder() {
        log.info("스마트발송 스케줄 실행: ATTENDANCE_REMINDER");
        smartPushService.executePush(SmartPushType.ATTENDANCE_REMINDER);
    }
}
