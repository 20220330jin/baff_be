package com.sa.baff.api;

import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.FastingRecordDto;
import com.sa.baff.model.vo.FastingRecordVO;
import com.sa.baff.service.FastingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fasting-records")
@RequiredArgsConstructor
public class FastingRecordRestController {

    private final FastingRecordService fastingRecordService;

    @PostMapping("/start")
    public FastingRecordDto.StartFastingResponse startFasting(
            @RequestBody FastingRecordVO.StartFasting param,
            @AuthenticationPrincipal String socialId) {
        return fastingRecordService.startFasting(param, socialId);
    }

    @PostMapping("/end")
    public FastingRecordDto.EndFastingResponse endFasting(
            @RequestBody FastingRecordVO.EndFasting param,
            @AuthenticationPrincipal String socialId) {
        return fastingRecordService.endFasting(param, socialId);
    }

    @GetMapping("/active")
    public FastingRecordDto.ActiveFasting getActiveFasting(
            @AuthenticationPrincipal String socialId) {
        return fastingRecordService.getActiveFasting(socialId);
    }

    @GetMapping("/list")
    public FastingRecordDto.GetFastingList getFastingList(
            @AuthenticationPrincipal String socialId) {
        return fastingRecordService.getFastingList(socialId);
    }

    @GetMapping("/analysis")
    public AiAnalysisDto.AnalysisResponse getAnalysis(
            @AuthenticationPrincipal String socialId) {
        return fastingRecordService.getAnalysis(socialId);
    }
}
