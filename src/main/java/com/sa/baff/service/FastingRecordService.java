package com.sa.baff.service;

import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.FastingRecordDto;
import com.sa.baff.model.vo.FastingRecordVO;

public interface FastingRecordService {

    FastingRecordDto.StartFastingResponse startFasting(FastingRecordVO.StartFasting param, String socialId);

    FastingRecordDto.EndFastingResponse endFasting(FastingRecordVO.EndFasting param, String socialId);

    FastingRecordDto.ActiveFasting getActiveFasting(String socialId);

    FastingRecordDto.GetFastingList getFastingList(String socialId);

    AiAnalysisDto.AnalysisResponse getAnalysis(String socialId);
}
