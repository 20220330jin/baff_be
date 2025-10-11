package com.sa.baff.service;

import com.sa.baff.common.AdminSyncStatus;
import com.sa.baff.domain.Inquiry;
import com.sa.baff.repository.InquiryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryAsyncService {

    private final InquiryRepository inquiryRepository;
    private final AdminApiClient adminApiClient;

    @Async
    @Transactional
    public void sendToAdminServer(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(() -> new IllegalArgumentException("Inquire not found with id: " + inquiryId));

        AdminSyncStatus syncStatus = adminApiClient.sendInquiry(inquiry);

        inquiry.updateAdminSyncStatus(syncStatus);
    }

}
