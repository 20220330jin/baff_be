package com.sa.baff.repository;

import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.vo.InquiryVO;

import java.util.List;

public interface InquiryRepositoryCustom {

    List<InquiryDto.getInquiryList> getInquiryList(InquiryVO.getInquiryListParam param, Long userId);

    InquiryDto.getInquiryList getInquiryDetail(Long userId, Long inquiryId);
}
