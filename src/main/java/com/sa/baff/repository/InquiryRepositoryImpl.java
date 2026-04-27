package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.Inquiry;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.vo.InquiryVO;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sa.baff.domain.QInquiry.inquiry;
import static com.sa.baff.domain.QInquiryReply.inquiryReply;
import static com.sa.baff.domain.QUserB.userB;

@Repository
public class InquiryRepositoryImpl extends QuerydslRepositorySupport implements InquiryRepositoryCustom {

    private JPAQueryFactory jpaQueryFactory;

    public InquiryRepositoryImpl(EntityManager entityManager) {
        super(Inquiry.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<InquiryDto.getInquiryList> getInquiryList(InquiryVO.getInquiryListParam param, Long userId) {
        // 1. DTO에서 필터 조건 추출
        String inquiryType = (param.getInquiryType() != null) ? String.valueOf(param.getInquiryType()) : null;
        String inquiryStatus = (param.getInquiryStatus() != null) ? String.valueOf(param.getInquiryStatus()) : null;

        // 2. 동적 필터링 조건 정의
        BooleanExpression isUserId = inquiry.user.id.eq(userId);
        BooleanExpression isDelYn = inquiry.delYn.eq('N');

        // inquiryType 동적 조건: null 또는 "ALL"인 경우 null 반환 (조건 무시)
        BooleanExpression inquiryTypeEq = (inquiryType != null && !"ALL".equalsIgnoreCase(inquiryType))
                ? inquiry.inquiryType.eq(InquiryType.valueOf(inquiryType))
                : null;

        // inquiryStatus 동적 조건: null 또는 "ALL"인 경우 null 반환 (조건 무시)
        BooleanExpression inquiryStatusEq = (inquiryStatus != null && !"ALL".equalsIgnoreCase(inquiryStatus))
                ? inquiry.status.eq(InquiryStatus.valueOf(inquiryStatus))
                : null;

        List<InquiryDto.getInquiryList> result = jpaQueryFactory
               .select(Projections.constructor(InquiryDto.getInquiryList.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.content,
                        inquiry.inquiryType,
                        inquiry.status,
                        inquiry.regDateTime
                ))
                .from(inquiry)
                .where(
                        isDelYn,
                        isUserId,
                        inquiryTypeEq,
                        inquiryStatusEq
                )
                .orderBy(inquiry.regDateTime.desc())
                .fetch();

        attachReplies(result);
        return result;
    }

    @Override
    public InquiryDto.getInquiryList getInquiryDetail(Long userId, Long inquiryId) {
        BooleanExpression isUserId = inquiry.user.id.eq(userId);
        BooleanExpression isDelYn = inquiry.delYn.eq('N');
        BooleanExpression isInquiryId = inquiry.id.eq(inquiryId);

        InquiryDto.getInquiryList result = jpaQueryFactory
                .select(Projections.constructor(InquiryDto.getInquiryList.class,
                                inquiry.id,
                                inquiry.title,
                                inquiry.content,
                                inquiry.inquiryType,
                                inquiry.status,
                                inquiry.regDateTime
                        ))
                .from(inquiry)
                .where(
                        isDelYn,
                        isUserId,
                        isInquiryId
                )
                .fetchFirst();

        if (result != null) {
            attachReplies(List.of(result));
        }
        return result;
    }

    @Override
    public List<InquiryDto.getAdminInquiryList> getAdminInquiryList(InquiryVO.getInquiryListParam param) {
        // 1. DTO에서 필터 조건 추출
        String inquiryType = (param.getInquiryType() != null) ? String.valueOf(param.getInquiryType()) : null;
        String inquiryStatus = (param.getInquiryStatus() != null) ? String.valueOf(param.getInquiryStatus()) : null;

        // 2. 동적 필터링 조건 정의
        BooleanExpression isDelYn = inquiry.delYn.eq('N');

        // inquiryType 동적 조건: null 또는 "ALL"인 경우 null 반환 (조건 무시)
        BooleanExpression inquiryTypeEq = (inquiryType != null && !"ALL".equalsIgnoreCase(inquiryType))
                ? inquiry.inquiryType.eq(InquiryType.valueOf(inquiryType))
                : null;

        // inquiryStatus 동적 조건: null 또는 "ALL"인 경우 null 반환 (조건 무시)
        BooleanExpression inquiryStatusEq = (inquiryStatus != null && !"ALL".equalsIgnoreCase(inquiryStatus))
                ? inquiry.status.eq(InquiryStatus.valueOf(inquiryStatus))
                : null;

        return jpaQueryFactory
                .select(Projections.constructor(InquiryDto.getAdminInquiryList.class,
                        inquiry.id,
                        userB.id,
                        userB.nickname,
                        inquiry.title,
                        inquiry.content,
                        inquiry.inquiryType,
                        inquiry.status,
                        inquiry.regDateTime
                ))
                .from(inquiry)
                .join(inquiry.user, userB)
                .where(
                        isDelYn,
                        inquiryTypeEq,
                        inquiryStatusEq
                )
                .orderBy(inquiry.regDateTime.desc())
                .fetch();
    }

    /**
     * 문의 목록/상세에 답변(InquiryReply) 일괄 매핑.
     * 한 번의 IN 쿼리로 N+1 회피.
     */
    private void attachReplies(List<InquiryDto.getInquiryList> inquiries) {
        if (inquiries == null || inquiries.isEmpty()) {
            return;
        }
        List<Long> inquiryIds = inquiries.stream()
                .map(InquiryDto.getInquiryList::getInquiryId)
                .toList();

        List<InquiryDto.InquiryReplyDto> replies = jpaQueryFactory
                .select(Projections.constructor(InquiryDto.InquiryReplyDto.class,
                        inquiryReply.id,
                        inquiryReply.inquiry.id,
                        inquiryReply.content,
                        inquiryReply.adminId,
                        inquiryReply.regDateTime
                ))
                .from(inquiryReply)
                .where(
                        inquiryReply.inquiry.id.in(inquiryIds),
                        inquiryReply.delYn.eq('N')
                )
                .orderBy(inquiryReply.regDateTime.asc())
                .fetch();

        Map<Long, List<InquiryDto.InquiryReplyDto>> repliesByInquiryId = replies.stream()
                .collect(Collectors.groupingBy(InquiryDto.InquiryReplyDto::getInquiryId));

        inquiries.forEach(item -> item.setReplies(
                repliesByInquiryId.getOrDefault(item.getInquiryId(), Collections.emptyList())
        ));
    }
}
