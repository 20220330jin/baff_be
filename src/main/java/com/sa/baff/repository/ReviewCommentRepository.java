package com.sa.baff.repository;

import com.sa.baff.domain.Review;
import com.sa.baff.domain.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long>, ReviewCommentRepositoryCustom {

    Long countByReviewAndDelYn(Review review, Character delYn);
}
