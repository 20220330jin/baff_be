package com.sa.baff.service;

import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.vo.InquiryVO;

import java.util.List;

public interface InquiryService {

    void createInquiry(InquiryVO.createInquiry param, String socialId);

    List<InquiryDto.getInquiryList> getInquiryList(InquiryVO.getInquiryListParam param, String socialId);
}
