package com.sa.baff.service;

import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.RunningRecordDto;
import com.sa.baff.model.vo.RunningRecordVO;

public interface RunningRecordService {

    Long recordRunning(RunningRecordVO.RecordRunning param, String socialId);

    RunningRecordDto.GetRunningList getRunningList(String socialId);

    AiAnalysisDto.AnalysisResponse getAnalysis(String socialId);
}
