package com.sa.baff.repository;

import com.sa.baff.domain.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {
    List<InquiryReply> findByInquiryIdOrderByRegDateTimeAsc(Long inquiryId);
}
