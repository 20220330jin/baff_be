package com.sa.baff.service;

import com.sa.baff.model.dto.AttendanceDto;

public interface AttendanceService {

    /** 오늘 출석 체크 */
    AttendanceDto.checkResponse checkAttendance(String socialId);

    /** 출석 현황 조회 (달력, 연속일수, canSaveStreak) */
    AttendanceDto.statusResponse getStatus(String socialId);

    /**
     * 출석 광고 보너스 지급 (S7-14).
     * 성공 시 추가 그램 지급 + AdWatchEvent 기록.
     * @param socialId 사용자 식별자
     * @param platform 호출 플랫폼 (현재 엔티티 스키마에 저장 안함 — 로깅용)
     * @param adFormat 광고 포맷(REWARDED/INTERSTITIAL/LEGACY) → AdWatchEvent.tossAdResponse에 저장
     */
    AttendanceDto.adBonusResponse grantAdBonus(String socialId, String platform, String adFormat);

    /**
     * 광고 시청으로 연속 출석 유지 (S7-14).
     * 조건: 오늘 미출석 + 어제 미출석 + 그저께 출석. 어제 날짜에 streakSaved=true 가상 출석 삽입.
     * 그램 지급 없음.
     */
    AttendanceDto.streakSaveResponse saveStreak(String socialId, String platform, String adFormat);
}
