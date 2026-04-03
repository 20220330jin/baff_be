package com.sa.baff.model.dto;

import com.sa.baff.util.AiFeatureType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class AiAnalysisDto {

    @Getter
    @Setter
    public static class AnalysisResponse {
        private AiFeatureType featureType;
        private String analysisHaiku;
        private String analysisSonnet;
        private LocalDateTime analyzedAt;
        private Boolean isStale; // 새 기록이 추가되어 재분석 필요한지
    }

    /** AI 기능 설정 (어드민) */
    @Getter
    @Setter
    public static class AiFeatureConfigResponse {
        private Long id;
        private AiFeatureType featureType;
        private Boolean enabled;
        private String description;
    }

    /** AI 기능 설정 목록 */
    @Getter
    @Setter
    public static class AiFeatureConfigList {
        private List<AiFeatureConfigResponse> configs;
    }
}
