package com.sa.baff.service;

import com.sa.baff.model.dto.RewardDto;

public interface ExchangeService {

    /** 그램 → 토스포인트 환전 */
    RewardDto.exchangeResponse exchange(String socialId, Integer amount, Boolean adWatched);
}
