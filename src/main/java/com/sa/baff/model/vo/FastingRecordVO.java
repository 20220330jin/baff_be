package com.sa.baff.model.vo;

import com.sa.baff.util.FastingMode;
import lombok.Getter;

public class FastingRecordVO {

    @Getter
    public static class StartFasting {
        private FastingMode mode;
        private Integer targetHours; // 커스텀 모드일 때만 사용
    }

    @Getter
    public static class EndFasting {
        private Long fastingRecordId;
    }
}
