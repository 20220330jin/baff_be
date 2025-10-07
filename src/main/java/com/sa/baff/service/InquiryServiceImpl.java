package com.sa.baff.service;

import com.sa.baff.domain.Inquiry;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.vo.InquiryVO;
import com.sa.baff.repository.InquiryRepository;
import com.sa.baff.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService{
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;

    @Override
    public void createInquiry(InquiryVO.createInquiry param, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        Inquiry inquiry = Inquiry.builder()
                .title(param.getTitle())
                .content(param.getContent())
                .inquiryType(param.getInquiryType())
                .status(InquiryStatus.RECEIVED)
                .user(user)
                .build();

        inquiryRepository.save(inquiry);
    }

    @Override
    public List<InquiryDto.getInquiryList> getInquiryList(InquiryVO.getInquiryListParam param, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));
        return inquiryRepository.getInquiryList(param, user.getId());
    }
}
