package com.sa.baff.service.account;

import com.sa.baff.model.dto.AccountLinkDto;

/**
 * 계정 통합 서비스 (spec §4.1~§4.4).
 * Primary socialId → issueLinkToken. 나머지는 Task 10~13에서 확장.
 */
public interface AccountLinkService {
    AccountLinkDto.IssueTokenResponse issueLinkToken(String primarySocialId);

    AccountLinkDto.PrepareResponse prepareLink(AccountLinkDto.PrepareRequest request);

    AccountLinkDto.ConfirmResponse confirmLink(AccountLinkDto.ConfirmRequest request);
}
