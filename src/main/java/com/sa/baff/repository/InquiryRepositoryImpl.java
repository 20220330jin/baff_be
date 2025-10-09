package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.Inquiry;
import com.sa.baff.domain.QInquiry;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import com.sa.baff.model.dto.InquiryDto;
import com.sa.baff.model.vo.InquiryVO;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.sa.baff.domain.QInquiry.inquiry;

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

        return jpaQueryFactory
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
    }

    @Override
    public InquiryDto.getInquiryList getInquiryDetail(Long userId, Long inquiryId) {
        BooleanExpression isUserId = inquiry.user.id.eq(userId);
        BooleanExpression isDelYn = inquiry.delYn.eq('N');
        BooleanExpression isInquiryId = inquiry.id.eq(inquiryId);

        return jpaQueryFactory
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
    }
}
