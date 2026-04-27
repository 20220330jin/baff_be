package com.sa.baff.repository;

import com.sa.baff.domain.SelfBannerAd;
import com.sa.baff.util.AdWatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelfBannerAdRepository extends JpaRepository<SelfBannerAd, Long> {

    List<SelfBannerAd> findByPositionAndEnabledAndDelYnOrderByPriorityAsc(
            AdWatchLocation position, Boolean enabled, Character delYn);

    List<SelfBannerAd> findByDelYnOrderByPriorityAsc(Character delYn);
}
