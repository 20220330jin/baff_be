package com.sa.baff.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 전체 타임존 관련 유틸
 */
public class DateTimeUtils {
    /**
     * UTC 기준 현재 시간
     */
    public static LocalDateTime now(){
      return LocalDateTime.now(ZoneOffset.UTC);
    }
}
