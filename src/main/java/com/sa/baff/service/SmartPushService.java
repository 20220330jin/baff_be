package com.sa.baff.service;

import com.sa.baff.util.SmartPushType;

import java.util.List;

public interface SmartPushService {

    /** 환전 미실행 사용자 대상 추출 */
    List<Long> findExchangeReminderTargets(int thresholdDays);

    /** 오늘 출석 미완료 사용자 대상 추출 */
    List<Long> findAttendanceReminderTargets();

    /** 스마트발송 실행 */
    void executePush(SmartPushType pushType);
}
