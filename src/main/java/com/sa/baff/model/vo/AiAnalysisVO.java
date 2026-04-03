package com.sa.baff.model.vo;

import com.sa.baff.util.AiFeatureType;
import lombok.Getter;

public class AiAnalysisVO {

    @Getter
    public static class UpdateAiFeatureConfig {
        private AiFeatureType featureType;
        private Boolean enabled;
        private String description;
    }
}
