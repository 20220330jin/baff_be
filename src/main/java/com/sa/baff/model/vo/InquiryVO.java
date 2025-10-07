package com.sa.baff.model.vo;

import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import lombok.Getter;
import lombok.Setter;

public class InquiryVO {

    @Getter
    @Setter
    public static class createInquiry {
        private String title;
        private String content;
        private InquiryType inquiryType;
    }

    @Getter
    @Setter
    public static class getInquiryListParam {
        private InquiryType inquiryType;
        private InquiryStatus inquiryStatus;
    }
}
