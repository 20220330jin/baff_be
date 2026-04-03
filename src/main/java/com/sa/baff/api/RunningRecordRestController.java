package com.sa.baff.api;

import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.RunningRecordDto;
import com.sa.baff.model.vo.RunningRecordVO;
import com.sa.baff.service.RunningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/running-records")
@RequiredArgsConstructor
public class RunningRecordRestController {

    private final RunningRecordService runningRecordService;

    @PostMapping("/record")
    public RunningRecordDto.RecordRunningResponse recordRunning(
            @RequestBody RunningRecordVO.RecordRunning param,
            @AuthenticationPrincipal String socialId) {
        Long id = runningRecordService.recordRunning(param, socialId);
        return new RunningRecordDto.RecordRunningResponse(id);
    }

    @GetMapping("/list")
    public RunningRecordDto.GetRunningList getRunningList(
            @AuthenticationPrincipal String socialId) {
        return runningRecordService.getRunningList(socialId);
    }

    @GetMapping("/analysis")
    public AiAnalysisDto.AnalysisResponse getAnalysis(
            @AuthenticationPrincipal String socialId) {
        return runningRecordService.getAnalysis(socialId);
    }
}
