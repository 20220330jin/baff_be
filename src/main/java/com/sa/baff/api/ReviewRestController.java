package com.sa.baff.api;

import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import com.sa.baff.service.R2Service;
import com.sa.baff.service.ReviewCommentService;
import com.sa.baff.service.ReviewLikeService;
import com.sa.baff.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewRestController {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;
    private final ReviewCommentService reviewCommentService;
    private final R2Service r2Service;

    @PostMapping("/uploadImages")
    public ResponseEntity<List<String>> uploadImages(@RequestParam("images") List<MultipartFile> files) {
        // 1. 파일 개수 유효성 검사
        if (files == null || files.isEmpty() || files.size() > 2) {
            throw new IllegalArgumentException("이미지는 최소 1장, 최대 2장까지만 업로드할 수 있습니다.");
        }
        // 2. R2Service 호출
        List<String> imageUrls = r2Service.uploadFiles(files);
        // 3. URL 목록 응답
        return ResponseEntity.ok(imageUrls);
    }

    @PostMapping("/createReview")
    public void createReview(@RequestBody ReviewVO.createReview createReviewParam, @AuthenticationPrincipal String socialId) {
        System.out.println("--------------------" + createReviewParam);
        reviewService.createReview(createReviewParam, socialId);
    }

    @GetMapping("/getReviewList")
    public ReviewDto.ReviewListResponse getReviewList(
            @AuthenticationPrincipal String socialId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reviewService.getReviewList(socialId, page, size);
    }

    @PostMapping("/toggleReviewLike/{reviewId}")
    public void toggleReviewLike(@PathVariable Long reviewId, @AuthenticationPrincipal String socialId) {
        reviewLikeService.toggleReviewLike(reviewId, socialId);
    }

    @PostMapping("/createComment")
    public void createComment(@RequestBody ReviewVO.createComment createComment, @AuthenticationPrincipal String socialId) {
        reviewCommentService.createComment(createComment, socialId);
    }

    @GetMapping("/getReviewCommentList/{reviewId}")
    public List<ReviewDto.getReviewCommentList> getReviewCommentList(@PathVariable Long reviewId, @AuthenticationPrincipal String socialId) {
        return reviewCommentService.getReviewCommentList(reviewId, socialId);
    }

    @PostMapping("/deleteReviewComment")
    public void deleteReviewComment(@RequestBody ReviewVO.deleteComment param, @AuthenticationPrincipal String socialId) {
        reviewCommentService.deleteReviewComment(param, socialId);
    }

    @PostMapping("/deleteReview/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId, @AuthenticationPrincipal String socialId) {
        reviewService.deleteReview(reviewId, socialId);
    }
}
