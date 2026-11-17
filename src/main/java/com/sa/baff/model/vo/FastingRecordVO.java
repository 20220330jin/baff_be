package com.sa.baff.model.vo;

import com.sa.baff.util.FastingMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class FastingRecordVO {

    @Getter
    @Setter
    public static class StartFasting {
        private FastingMode mode;
        private Integer targetHours; // 커스텀 모드일 때만 사용
    }

    @Getter
    @Setter
    public static class EndFasting {
        private Long fastingRecordId;
        /** 식사 시각 (M-043 · KST · 선택) — 없으면 지금 시각. 시작 후 48시간이 지났으면 필수 */
        private LocalDateTime endTime;
        /** 「기억 안 나요」 (M-043 · 선택) — true 면 종료 시각 미확인으로 남긴다 */
        private Boolean unknown;
    }
}
