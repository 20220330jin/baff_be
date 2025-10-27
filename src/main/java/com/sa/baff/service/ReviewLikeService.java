package com.sa.baff.service;

public interface ReviewLikeService {
    /**
     * 리뷰에 대한 좋아요를 토글합니다. (누르기 / 취소)
     * @param reviewId 좋아요 대상 리뷰 ID
     * @param socialId 요청 사용자 소셜 ID
     * @return 좋아요 상태 (true: 좋아요, false: 좋아요 취소)
     */
    boolean toggleReviewLike(Long reviewId, String socialId);
}