package com.sa.baff.api;

import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.dto.ResponseDto;
import com.sa.baff.model.vo.InquiryVO;
import com.sa.baff.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
public class InquiryRestController {
    private final InquiryService inquiryService;

    @PostMapping("/createInquiry")
    public ResponseEntity<ResponseDto> createInquiry(@RequestBody InquiryVO.createInquiry createInquiryParam, @AuthenticationPrincipal String socialId) {
        inquiryService.createInquiry(createInquiryParam, socialId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto());
    }

    @GetMapping("/getInquiryList")
    public List<InquiryDto.getInquiryList> getInquiryList(@ModelAttribute InquiryVO.getInquiryListParam param, @AuthenticationPrincipal String socialId) {
        return inquiryService.getInquiryList(param, socialId);
    }

    @GetMapping("/getInquiryDetail/{inquiryId}")
    public InquiryDto.getInquiryList getInquiryDetail(@PathVariable Long inquiryId, @AuthenticationPrincipal String socialId) {
        return inquiryService.getInquiryDetail(inquiryId, socialId);
    }
}
