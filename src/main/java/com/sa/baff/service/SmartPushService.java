package com.sa.baff.service;

import com.sa.baff.model.SmartPushRecipient;
import com.sa.baff.util.SmartPushType;

import java.util.List;

public interface SmartPushService {

    /** 토스 발송 가능 유저 전체 (직접 토스 + 병합 Primary) — spec §3.2 */
    List<SmartPushRecipient> findAllTossRecipients();

    /** 최근 7일 활동 + 오늘 미출석 Recipient 추출 */
    List<SmartPushRecipient> findAttendanceReminderRecipients();

    /** 그램 100g 이상 보유 Recipient 추출 */
    List<SmartPushRecipient> findExchangeReminderRecipients(int thresholdDays);

    /** 스마트발송 실행 */
    void executePush(SmartPushType pushType);
}
