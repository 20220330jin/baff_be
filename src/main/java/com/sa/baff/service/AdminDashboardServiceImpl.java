package com.sa.baff.service;

import com.sa.baff.domain.*;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.model.dto.AdminDashboardDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.AdWatchLocation;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final WeightRepository weightRepository;
    private final GoalsRepository goalsRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final ReviewRepository reviewRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final NoticeRepository noticeRepository;
    private final PieceRepository pieceRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final ExchangeHistoryRepository exchangeHistoryRepository;
    private final AdWatchEventRepository adWatchEventRepository;
    private final AdPositionConfigRepository adPositionConfigRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== 대시보드 개요 =====

    @Override
    public AdminDashboardDto.AdminStats getAdminStats() {
        List<UserB> allUsers = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        long totalUsers = allUsers.size();

        LocalDateTime oneWeekAgo = DateTimeUtils.now().minusWeeks(1);
        long newUsersThisWeek = allUsers.stream()
                .filter(u -> u.getRegDateTime() != null && u.getRegDateTime().isAfter(oneWeekAgo))
                .count();

        long activeBattles = battleRoomRepository.findAll().stream()
                .filter(b -> b.getStatus() == BattleStatus.IN_PROGRESS || b.getStatus() == BattleStatus.WAITING)
                .count();

        long totalReviews = reviewRepository.count();

        List<Inquiry> allInquiries = inquiryRepository.findAll();
        long totalInquiries = allInquiries.size();
        long pendingInquiries = allInquiries.stream()
                .filter(i -> i.getStatus() == InquiryStatus.RECEIVED || i.getStatus() == InquiryStatus.IN_PROGRESS)
                .count();

        return AdminDashboardDto.AdminStats.builder()
                .totalUsers(totalUsers)
                .newUsersThisWeek(newUsersThisWeek)
                .activeBattles(activeBattles)
                .totalReviews(totalReviews)
                .totalInquiries(totalInquiries)
                .pendingInquiries(pendingInquiries)
                .build();
    }

    @Override
    public List<AdminDashboardDto.UserGrowth> getUserGrowth(String period) {
        List<UserB> allUsers = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        LocalDateTime now = DateTimeUtils.now();

        switch (period.toUpperCase()) {
            case "DAILY": {
                // 최근 30일 일별
                List<AdminDashboardDto.UserGrowth> result = new ArrayList<>();
                for (int i = 29; i >= 0; i--) {
                    LocalDate date = now.toLocalDate().minusDays(i);
                    long count = allUsers.stream()
                            .filter(u -> u.getRegDateTime() != null
                                    && u.getRegDateTime().toLocalDate().equals(date))
                            .count();
                    result.add(AdminDashboardDto.UserGrowth.builder()
                            .label(date.format(DATE_FORMATTER))
                            .count(count)
                            .build());
                }
                return result;
            }
            case "WEEKLY": {
                // 최근 12주 주간
                List<AdminDashboardDto.UserGrowth> result = new ArrayList<>();
                for (int i = 11; i >= 0; i--) {
                    LocalDate weekStart = now.toLocalDate().minusWeeks(i)
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate weekEnd = weekStart.plusDays(6);
                    long count = allUsers.stream()
                            .filter(u -> u.getRegDateTime() != null
                                    && !u.getRegDateTime().toLocalDate().isBefore(weekStart)
                                    && !u.getRegDateTime().toLocalDate().isAfter(weekEnd))
                            .count();
                    result.add(AdminDashboardDto.UserGrowth.builder()
                            .label(weekStart.format(DATE_FORMATTER) + " ~ " + weekEnd.format(DATE_FORMATTER))
                            .count(count)
                            .build());
                }
                return result;
            }
            case "MONTHLY": {
                // 최근 12개월 월간
                List<AdminDashboardDto.UserGrowth> result = new ArrayList<>();
                for (int i = 11; i >= 0; i--) {
                    LocalDate monthStart = now.toLocalDate().minusMonths(i).withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                    long count = allUsers.stream()
                            .filter(u -> u.getRegDateTime() != null
                                    && !u.getRegDateTime().toLocalDate().isBefore(monthStart)
                                    && !u.getRegDateTime().toLocalDate().isAfter(monthEnd))
                            .count();
                    result.add(AdminDashboardDto.UserGrowth.builder()
                            .label(monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                            .count(count)
                            .build());
                }
                return result;
            }
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public List<AdminDashboardDto.WeightTrend> getWeightTrend(String period) {
        List<Weight> allWeights = weightRepository.findAll();
        LocalDateTime now = DateTimeUtils.now();

        switch (period.toUpperCase()) {
            case "DAILY": {
                List<AdminDashboardDto.WeightTrend> result = new ArrayList<>();
                for (int i = 29; i >= 0; i--) {
                    LocalDate date = now.toLocalDate().minusDays(i);
                    long count = allWeights.stream()
                            .filter(w -> w.getRecordDate() != null
                                    && w.getRecordDate().toLocalDate().equals(date))
                            .count();
                    result.add(AdminDashboardDto.WeightTrend.builder()
                            .label(date.format(DATE_FORMATTER))
                            .count(count)
                            .build());
                }
                return result;
            }
            case "WEEKLY": {
                List<AdminDashboardDto.WeightTrend> result = new ArrayList<>();
                for (int i = 11; i >= 0; i--) {
                    LocalDate weekStart = now.toLocalDate().minusWeeks(i)
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate weekEnd = weekStart.plusDays(6);
                    long count = allWeights.stream()
                            .filter(w -> w.getRecordDate() != null
                                    && !w.getRecordDate().toLocalDate().isBefore(weekStart)
                                    && !w.getRecordDate().toLocalDate().isAfter(weekEnd))
                            .count();
                    result.add(AdminDashboardDto.WeightTrend.builder()
                            .label(weekStart.format(DATE_FORMATTER) + " ~ " + weekEnd.format(DATE_FORMATTER))
                            .count(count)
                            .build());
                }
                return result;
            }
            case "MONTHLY": {
                List<AdminDashboardDto.WeightTrend> result = new ArrayList<>();
                for (int i = 11; i >= 0; i--) {
                    LocalDate monthStart = now.toLocalDate().minusMonths(i).withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                    long count = allWeights.stream()
                            .filter(w -> w.getRecordDate() != null
                                    && !w.getRecordDate().toLocalDate().isBefore(monthStart)
                                    && !w.getRecordDate().toLocalDate().isAfter(monthEnd))
                            .count();
                    result.add(AdminDashboardDto.WeightTrend.builder()
                            .label(monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                            .count(count)
                            .build());
                }
                return result;
            }
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public List<AdminDashboardDto.PlatformDistribution> getPlatformDistribution() {
        List<UserB> allUsers = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        // provider별 분포
        Map<String, Long> providerMap = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getProvider() != null ? u.getProvider() : "unknown",
                        Collectors.counting()));

        // platform별 분포
        Map<String, Long> platformMap = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getPlatform() != null ? u.getPlatform() : "unknown",
                        Collectors.counting()));

        List<AdminDashboardDto.PlatformDistribution> result = new ArrayList<>();

        providerMap.forEach((key, value) ->
                result.add(AdminDashboardDto.PlatformDistribution.builder()
                        .platform("provider:" + key)
                        .count(value)
                        .build()));

        platformMap.forEach((key, value) ->
                result.add(AdminDashboardDto.PlatformDistribution.builder()
                        .platform("platform:" + key)
                        .count(value)
                        .build()));

        return result;
    }

    @Override
    public List<AdminDashboardDto.RecentActivity> getRecentActivities() {
        List<AdminDashboardDto.RecentActivity> activities = new ArrayList<>();

        // 최근 가입 사용자
        StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(u -> u.getRegDateTime() != null)
                .sorted(Comparator.comparing(UserB::getRegDateTime).reversed())
                .limit(5)
                .forEach(u -> activities.add(AdminDashboardDto.RecentActivity.builder()
                        .type("USER_JOIN")
                        .message(u.getNickname() + "님이 가입했습니다.")
                        .timestamp(u.getRegDateTime().format(DATETIME_FORMATTER))
                        .build()));

        // 최근 문의
        inquiryRepository.findAll().stream()
                .filter(i -> i.getRegDateTime() != null)
                .sorted(Comparator.comparing(Inquiry::getRegDateTime).reversed())
                .limit(3)
                .forEach(i -> activities.add(AdminDashboardDto.RecentActivity.builder()
                        .type("INQUIRY")
                        .message("새 문의: " + i.getTitle())
                        .timestamp(i.getRegDateTime().format(DATETIME_FORMATTER))
                        .build()));

        // 최근 배틀 생성
        battleRoomRepository.findAll().stream()
                .filter(b -> b.getRegDateTime() != null)
                .sorted(Comparator.comparing(BattleRoom::getRegDateTime).reversed())
                .limit(2)
                .forEach(b -> activities.add(AdminDashboardDto.RecentActivity.builder()
                        .type("BATTLE_CREATED")
                        .message("새 배틀: " + b.getName())
                        .timestamp(b.getRegDateTime().format(DATETIME_FORMATTER))
                        .build()));

        // 전체를 timestamp 기준 내림차순 정렬 후 최근 10건
        return activities.stream()
                .sorted(Comparator.comparing(AdminDashboardDto.RecentActivity::getTimestamp).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    // ===== 사용자 관리 =====

    @Override
    public Page<AdminDashboardDto.AdminUserListItem> getUsers(String status, String search, Pageable pageable) {
        List<UserB> allUsers = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        // status 필터
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
            allUsers = allUsers.stream()
                    .filter(u -> u.getStatus() == userStatus)
                    .collect(Collectors.toList());
        }

        // search 필터 (닉네임/이메일)
        if (search != null && !search.isEmpty()) {
            String keyword = search.toLowerCase();
            allUsers = allUsers.stream()
                    .filter(u -> (u.getNickname() != null && u.getNickname().toLowerCase().contains(keyword))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }

        // 최신순 정렬
        allUsers.sort(Comparator.comparing(
                (UserB u) -> u.getRegDateTime() != null ? u.getRegDateTime() : LocalDateTime.MIN).reversed());

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allUsers.size());
        List<UserB> pageContent = start > allUsers.size() ? Collections.emptyList() : allUsers.subList(start, end);

        List<AdminDashboardDto.AdminUserListItem> items = pageContent.stream()
                .map(this::toUserListItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, allUsers.size());
    }

    @Override
    public AdminDashboardDto.AdminUserDetail getUserDetail(Long userId) {
        UserB user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        long totalWeightRecords = user.getWeights() != null ? user.getWeights().size() : 0;
        long totalGoals = user.getGoals() != null ? user.getGoals().size() : 0;

        long totalBattles = battleRoomRepository.findAll().stream()
                .filter(b -> b.getParticipants().stream()
                        .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(userId)))
                .count();

        long totalReviews = reviewRepository.findAll().stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId))
                .count();

        long pieceBalance = pieceRepository.findByUser(user)
                .map(Piece::getBalance)
                .orElse(0L);

        return AdminDashboardDto.AdminUserDetail.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .provider(user.getProvider())
                .platform(user.getPlatform())
                .height(user.getHeight())
                .regDateTime(user.getRegDateTime() != null ? user.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .totalWeightRecords(totalWeightRecords)
                .totalGoals(totalGoals)
                .totalBattles(totalBattles)
                .totalReviews(totalReviews)
                .pieceBalance(pieceBalance)
                .build();
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role) {
        UserB user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
        user.setRole(Role.valueOf(role.toUpperCase()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        UserB user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
        user.setStatus(UserStatus.valueOf(status.toUpperCase()));
        userRepository.save(user);
    }

    // ===== 문의 관리 =====

    @Override
    public Page<AdminDashboardDto.AdminInquiryListItem> getInquiries(String status, String type, String search, Pageable pageable) {
        List<Inquiry> allInquiries = inquiryRepository.findAll();

        // status 필터
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            InquiryStatus inquiryStatus = InquiryStatus.valueOf(status.toUpperCase());
            allInquiries = allInquiries.stream()
                    .filter(i -> i.getStatus() == inquiryStatus)
                    .collect(Collectors.toList());
        }

        // type 필터
        if (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            InquiryType inquiryType = InquiryType.valueOf(type.toUpperCase());
            allInquiries = allInquiries.stream()
                    .filter(i -> i.getInquiryType() == inquiryType)
                    .collect(Collectors.toList());
        }

        // search 필터 (제목/내용/닉네임)
        if (search != null && !search.isEmpty()) {
            String keyword = search.toLowerCase();
            allInquiries = allInquiries.stream()
                    .filter(i -> (i.getTitle() != null && i.getTitle().toLowerCase().contains(keyword))
                            || (i.getContent() != null && i.getContent().toLowerCase().contains(keyword))
                            || (i.getUser() != null && i.getUser().getNickname() != null
                            && i.getUser().getNickname().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }

        // 최신순 정렬
        allInquiries.sort(Comparator.comparing(
                (Inquiry i) -> i.getRegDateTime() != null ? i.getRegDateTime() : LocalDateTime.MIN).reversed());

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allInquiries.size());
        List<Inquiry> pageContent = start > allInquiries.size() ? Collections.emptyList() : allInquiries.subList(start, end);

        List<AdminDashboardDto.AdminInquiryListItem> items = pageContent.stream()
                .map(this::toInquiryListItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, allInquiries.size());
    }

    @Override
    public AdminDashboardDto.AdminInquiryDetail getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + inquiryId));

        List<InquiryReply> replies = inquiryReplyRepository.findByInquiryIdOrderByRegDateTimeAsc(inquiryId);

        List<AdminDashboardDto.InquiryReplyItem> replyItems = replies.stream()
                .map(r -> {
                    // 관리자 닉네임 조회
                    String adminNickname = userRepository.findById(r.getAdminId())
                            .map(UserB::getNickname)
                            .orElse("관리자");

                    return AdminDashboardDto.InquiryReplyItem.builder()
                            .replyId(r.getId())
                            .content(r.getContent())
                            .adminId(r.getAdminId())
                            .adminNickname(adminNickname)
                            .regDateTime(r.getRegDateTime() != null ? r.getRegDateTime().format(DATETIME_FORMATTER) : null)
                            .build();
                })
                .collect(Collectors.toList());

        UserB user = inquiry.getUser();

        return AdminDashboardDto.AdminInquiryDetail.builder()
                .inquiryId(inquiry.getId())
                .userId(user != null ? user.getId() : null)
                .nickname(user != null ? user.getNickname() : null)
                .email(user != null ? user.getEmail() : null)
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType() != null ? inquiry.getInquiryType().name() : null)
                .inquiryStatus(inquiry.getStatus() != null ? inquiry.getStatus().name() : null)
                .regDateTime(inquiry.getRegDateTime() != null ? inquiry.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .replies(replyItems)
                .build();
    }

    @Override
    @Transactional
    public void replyToInquiry(Long inquiryId, String content, String adminSocialId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + inquiryId));

        // 답변 작성자는 Resolver 경유로 활성 Primary만 허용 (탈퇴/병합 계정 차단 — spec §9.3)
        UserB admin = accountLinkedUserResolver.resolveActiveUserBySocialId(adminSocialId)
                .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다. socialId=" + adminSocialId));

        InquiryReply reply = new InquiryReply(inquiry, content, admin.getId());
        inquiryReplyRepository.save(reply);
        inquiry.addReply(reply);

        // 상태를 ANSWERED로 변경
        inquiry.updateStatus(InquiryStatus.ANSWERED);
    }

    @Override
    @Transactional
    public void updateInquiryStatus(Long inquiryId, String status) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + inquiryId));
        inquiry.updateStatus(InquiryStatus.valueOf(status.toUpperCase()));
    }

    // ===== 배틀 관리 =====

    @Override
    public Page<AdminDashboardDto.AdminBattleListItem> getBattles(String status, Pageable pageable) {
        List<BattleRoom> allBattles = battleRoomRepository.findAll();

        // status 필터
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            BattleStatus battleStatus = BattleStatus.valueOf(status.toUpperCase());
            allBattles = allBattles.stream()
                    .filter(b -> b.getStatus() == battleStatus)
                    .collect(Collectors.toList());
        }

        // 최신순 정렬
        allBattles.sort(Comparator.comparing(
                (BattleRoom b) -> b.getRegDateTime() != null ? b.getRegDateTime() : LocalDateTime.MIN).reversed());

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allBattles.size());
        List<BattleRoom> pageContent = start > allBattles.size() ? Collections.emptyList() : allBattles.subList(start, end);

        List<AdminDashboardDto.AdminBattleListItem> items = pageContent.stream()
                .map(this::toBattleListItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, allBattles.size());
    }

    // ===== 리뷰 관리 =====

    @Override
    public Page<AdminDashboardDto.AdminReviewListItem> getReviews(String search, Pageable pageable) {
        List<Review> allReviews = reviewRepository.findAll();

        // search 필터 (제목/작성자 닉네임)
        if (search != null && !search.isEmpty()) {
            String keyword = search.toLowerCase();
            allReviews = allReviews.stream()
                    .filter(r -> (r.getTitle() != null && r.getTitle().toLowerCase().contains(keyword))
                            || (r.getUser() != null && r.getUser().getNickname() != null
                            && r.getUser().getNickname().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }

        // 최신순 정렬
        allReviews.sort(Comparator.comparing(
                (Review r) -> r.getRegDateTime() != null ? r.getRegDateTime() : LocalDateTime.MIN).reversed());

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allReviews.size());
        List<Review> pageContent = start > allReviews.size() ? Collections.emptyList() : allReviews.subList(start, end);

        List<AdminDashboardDto.AdminReviewListItem> items = pageContent.stream()
                .map(this::toReviewListItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, allReviews.size());
    }

    @Override
    @Transactional
    public void updateReviewVisibility(Long reviewId, boolean isPublic) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. id=" + reviewId));
        review.setIsPublic(isPublic);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. id=" + reviewId));
        reviewRepository.delete(review);
    }

    // ===== 공지사항 =====

    @Override
    public List<AdminDashboardDto.NoticeItem> getNotices() {
        return noticeRepository.findAllByOrderByRegDateTimeDesc().stream()
                .map(this::toNoticeItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminDashboardDto.NoticeItem createNotice(String title, String content) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .isActive(true)
                .build();
        Notice saved = noticeRepository.save(notice);
        return toNoticeItem(saved);
    }

    @Override
    @Transactional
    public AdminDashboardDto.NoticeItem updateNotice(Long noticeId, String title, String content, Boolean isActive) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. id=" + noticeId));
        notice.update(title, content, isActive);
        Notice saved = noticeRepository.save(notice);
        return toNoticeItem(saved);
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new EntityNotFoundException("공지사항을 찾을 수 없습니다. id=" + noticeId);
        }
        noticeRepository.deleteById(noticeId);
    }

    // ===== Private 매핑 메서드 =====

    private AdminDashboardDto.AdminUserListItem toUserListItem(UserB user) {
        return AdminDashboardDto.AdminUserListItem.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .provider(user.getProvider())
                .platform(user.getPlatform())
                .height(user.getHeight())
                .regDateTime(user.getRegDateTime() != null ? user.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .build();
    }

    private AdminDashboardDto.AdminInquiryListItem toInquiryListItem(Inquiry inquiry) {
        UserB user = inquiry.getUser();
        return AdminDashboardDto.AdminInquiryListItem.builder()
                .inquiryId(inquiry.getId())
                .userId(user != null ? user.getId() : null)
                .nickname(user != null ? user.getNickname() : null)
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType() != null ? inquiry.getInquiryType().name() : null)
                .inquiryStatus(inquiry.getStatus() != null ? inquiry.getStatus().name() : null)
                .regDateTime(inquiry.getRegDateTime() != null ? inquiry.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .replyCount(inquiry.getReplies() != null ? inquiry.getReplies().size() : 0)
                .build();
    }

    private AdminDashboardDto.AdminBattleListItem toBattleListItem(BattleRoom battle) {
        return AdminDashboardDto.AdminBattleListItem.builder()
                .battleId(battle.getId())
                .entryCode(battle.getEntryCode())
                .name(battle.getName())
                .hostNickname(battle.getHost() != null ? battle.getHost().getNickname() : null)
                .participantCount(battle.getParticipants() != null ? battle.getParticipants().size() : 0)
                .maxParticipants(battle.getMaxParticipants() != null ? battle.getMaxParticipants() : 0)
                .status(battle.getStatus() != null ? battle.getStatus().name() : null)
                .durationDays(battle.getDurationDays())
                .betAmount(battle.getBetAmount())
                .startDate(battle.getStartDate() != null ? battle.getStartDate().format(DATE_FORMATTER) : null)
                .endDate(battle.getEndDate() != null ? battle.getEndDate().format(DATE_FORMATTER) : null)
                .regDateTime(battle.getRegDateTime() != null ? battle.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .build();
    }

    private AdminDashboardDto.AdminReviewListItem toReviewListItem(Review review) {
        return AdminDashboardDto.AdminReviewListItem.builder()
                .reviewId(review.getId())
                .title(review.getTitle())
                .authorNickname(review.getUser() != null ? review.getUser().getNickname() : null)
                .difficulty(review.getDifficulty())
                .dietMethods(review.getDietMethods())
                .isPublic(Boolean.TRUE.equals(review.getIsPublic()))
                .likes(review.getLikes() != null ? review.getLikes() : 0L)
                .commentCount(review.getCommentCount() != null ? review.getCommentCount() : 0L)
                .regDateTime(review.getRegDateTime() != null ? review.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .build();
    }

    private AdminDashboardDto.NoticeItem toNoticeItem(Notice notice) {
        return AdminDashboardDto.NoticeItem.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isActive(notice.getIsActive())
                .regDateTime(notice.getRegDateTime() != null ? notice.getRegDateTime().format(DATETIME_FORMATTER) : null)
                .modDateTime(notice.getModDateTime() != null ? notice.getModDateTime().format(DATETIME_FORMATTER) : null)
                .build();
    }

    // ==================== 리워드 관리 ====================

    @Override
    public AdminDashboardDto.AdminRewardSummary getRewardSummary() {
        List<Piece> allPieces = pieceRepository.findAll();
        long totalEarned = allPieces.stream().mapToLong(Piece::getTotalEarned).sum();
        long totalExchanged = allPieces.stream().mapToLong(Piece::getTotalExchanged).sum();
        long currentBalance = allPieces.stream().mapToLong(Piece::getBalance).sum();
        long activeUsers = allPieces.stream().filter(p -> p.getBalance() > 0 || p.getTotalEarned() > 0).count();

        List<ExchangeHistory> allExchanges = exchangeHistoryRepository.findAll();
        long totalExchangeAmount = allExchanges.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus().name()))
                .mapToLong(ExchangeHistory::getTossAmount)
                .sum();
        long pendingExchanges = allExchanges.stream()
                .filter(e -> "PENDING".equals(e.getStatus().name()))
                .count();

        LocalDate today = LocalDate.now();
        List<RewardHistory> allRewards = rewardHistoryRepository.findAll();
        long todayIssued = allRewards.stream()
                .filter(r -> r.getRegDateTime() != null && r.getRegDateTime().toLocalDate().equals(today))
                .filter(r -> "SUCCESS".equals(r.getStatus().name()))
                .mapToLong(RewardHistory::getAmount)
                .sum();

        return AdminDashboardDto.AdminRewardSummary.builder()
                .totalIssuedPieces(totalEarned)
                .totalBurnedPieces(totalExchanged)
                .currentCirculating(currentBalance)
                .totalExchangeAmount(totalExchangeAmount)
                .todayIssuedPieces(todayIssued)
                .todayBurnedPieces(0)
                .activeRewardUsers(activeUsers)
                .pendingExchanges(pendingExchanges)
                .build();
    }

    @Override
    public Page<AdminDashboardDto.AdminRewardConfigItem> getRewardConfigs(Pageable pageable) {
        List<RewardConfig> configs = rewardConfigRepository.findAll();
        List<AdminDashboardDto.AdminRewardConfigItem> items = configs.stream()
                .map(c -> AdminDashboardDto.AdminRewardConfigItem.builder()
                        .configId(c.getId())
                        .rewardType(c.getRewardType() != null ? c.getRewardType().name() : null)
                        .actionType(c.getRewardType() != null ? c.getRewardType().name() : null)
                        .pieceAmount(c.getAmount())
                        .description(c.getDescription())
                        .isActive(Boolean.TRUE.equals(c.getEnabled()))
                        .regDateTime(c.getRegDateTime() != null ? c.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .modDateTime(c.getModDateTime() != null ? c.getModDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(start < items.size() ? items.subList(start, end) : List.of(), pageable, items.size());
    }

    @Override
    public Page<AdminDashboardDto.AdminExchangeItem> getRewardExchanges(Pageable pageable) {
        List<ExchangeHistory> allExchanges = exchangeHistoryRepository.findAll();
        allExchanges.sort((a, b) -> b.getRegDateTime().compareTo(a.getRegDateTime()));

        Set<Long> userIds = allExchanges.stream().map(ExchangeHistory::getUserId).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = buildNicknameMap(userIds);

        List<AdminDashboardDto.AdminExchangeItem> items = allExchanges.stream()
                .map(e -> AdminDashboardDto.AdminExchangeItem.builder()
                        .exchangeId(e.getId())
                        .userId(e.getUserId())
                        .nickname(nicknameMap.getOrDefault(e.getUserId(), "-"))
                        .pieceAmount(e.getPointAmount())
                        .exchangeAmount(e.getTossAmount())
                        .status(e.getStatus() != null ? e.getStatus().name() : null)
                        .regDateTime(e.getRegDateTime() != null ? e.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(start < items.size() ? items.subList(start, end) : List.of(), pageable, items.size());
    }

    // ==================== 내역 관리 ====================

    @Override
    public Page<AdminDashboardDto.LoginHistoryItem> getLoginHistories(Pageable pageable) {
        Page<LoginHistory> page = loginHistoryRepository.findAll(pageable);
        List<AdminDashboardDto.LoginHistoryItem> items = page.getContent().stream()
                .map(lh -> AdminDashboardDto.LoginHistoryItem.builder()
                        .id(lh.getId())
                        .userId(lh.getUser() != null ? lh.getUser().getId() : null)
                        .nickname(lh.getUser() != null ? lh.getUser().getNickname() : "-")
                        .userAgent(lh.getRawUserAgent())
                        .loginDateTime(lh.getRegDateTime() != null ? lh.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    @Override
    public Page<AdminDashboardDto.WeightHistoryItem> getWeightHistories(Pageable pageable) {
        Page<Weight> page = weightRepository.findAll(pageable);
        List<AdminDashboardDto.WeightHistoryItem> items = page.getContent().stream()
                .map(w -> AdminDashboardDto.WeightHistoryItem.builder()
                        .id(w.getId())
                        .userId(w.getUser() != null ? w.getUser().getId() : null)
                        .nickname(w.getUser() != null ? w.getUser().getNickname() : "-")
                        .weight(w.getWeight())
                        .recordDate(w.getRecordDate() != null ? w.getRecordDate().format(DATETIME_FORMATTER) : null)
                        .regDateTime(w.getRegDateTime() != null ? w.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    /** userId → nickname 맵 빌드 (배치) */
    private Map<Long, String> buildNicknameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<UserB> users = (List<UserB>) userRepository.findAllByIdIn(new ArrayList<>(userIds));
        return users.stream().collect(Collectors.toMap(UserB::getId, u -> u.getNickname() != null ? u.getNickname() : "-"));
    }

    // ==================== 광고 시청 관리 ====================

    @Override
    public AdminDashboardDto.AdWatchSummary getAdWatchSummary() {
        List<AdWatchEvent> allEvents = adWatchEventRepository.findAll();
        long total = allEvents.size();
        long uniqueUsers = allEvents.stream().map(AdWatchEvent::getUserId).distinct().count();

        LocalDate today = LocalDate.now();
        List<AdWatchEvent> todayEvents = allEvents.stream()
                .filter(e -> e.getRegDateTime() != null && e.getRegDateTime().toLocalDate().equals(today))
                .collect(Collectors.toList());
        long todayCount = todayEvents.size();
        long todayUnique = todayEvents.stream().map(AdWatchEvent::getUserId).distinct().count();

        Map<String, Long> locationCounts = allEvents.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getWatchLocation() != null ? e.getWatchLocation().name() : "UNKNOWN",
                        Collectors.counting()));
        List<AdminDashboardDto.AdWatchLocationStat> locationStats = locationCounts.entrySet().stream()
                .map(entry -> AdminDashboardDto.AdWatchLocationStat.builder()
                        .location(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        return AdminDashboardDto.AdWatchSummary.builder()
                .totalWatchCount(total)
                .todayWatchCount(todayCount)
                .uniqueUsers(uniqueUsers)
                .todayUniqueUsers(todayUnique)
                .locationStats(locationStats)
                .build();
    }

    @Override
    public Page<AdminDashboardDto.AdWatchHistoryItem> getAdWatchHistory(Pageable pageable) {
        List<AdWatchEvent> allEvents = adWatchEventRepository.findAll();
        allEvents.sort((a, b) -> b.getRegDateTime().compareTo(a.getRegDateTime()));

        Set<Long> userIds = allEvents.stream().map(AdWatchEvent::getUserId).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = buildNicknameMap(userIds);

        List<AdminDashboardDto.AdWatchHistoryItem> items = allEvents.stream()
                .map(e -> AdminDashboardDto.AdWatchHistoryItem.builder()
                        .id(e.getId())
                        .userId(e.getUserId())
                        .nickname(nicknameMap.getOrDefault(e.getUserId(), "-"))
                        .watchLocation(e.getWatchLocation() != null ? e.getWatchLocation().name() : null)
                        .referenceId(e.getReferenceId())
                        .tossAdResponse(e.getTossAdResponse())
                        .regDateTime(e.getRegDateTime() != null ? e.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(start < items.size() ? items.subList(start, end) : List.of(), pageable, items.size());
    }

    // ==================== 토스광고 설정 ====================

    @Override
    public List<AdminDashboardDto.TossAdPositionConfig> getTossAdConfigs() {
        return adPositionConfigRepository.findAll().stream()
                .map(c -> AdminDashboardDto.TossAdPositionConfig.builder()
                        .id(c.getId())
                        .position(c.getPosition().name())
                        .tossAdRatio(c.getTossAdRatio())
                        .tossAdGroupId(c.getTossAdGroupId())
                        .isTossAdEnabled(c.getIsTossAdEnabled())
                        .tossImageAdGroupId(c.getTossImageAdGroupId())
                        .tossImageAdRatio(c.getTossImageAdRatio())
                        .isTossImageAdEnabled(c.getIsTossImageAdEnabled())
                        .tossBannerAdGroupId(c.getTossBannerAdGroupId())
                        .tossBannerAdRatio(c.getTossBannerAdRatio())
                        .isTossBannerAdEnabled(c.getIsTossBannerAdEnabled())
                        .tossInterstitialAdGroupId(c.getTossInterstitialAdGroupId())
                        .isTossInterstitialAdEnabled(c.getIsTossInterstitialAdEnabled())
                        .rewardedAdRatio(c.getRewardedAdRatio())
                        .rewardedAdGrams(c.getRewardedAdGrams())
                        .interstitialAdGrams(c.getInterstitialAdGrams())
                        .regDateTime(c.getRegDateTime() != null ? c.getRegDateTime().format(DATETIME_FORMATTER) : null)
                        .modDateTime(c.getModDateTime() != null ? c.getModDateTime().format(DATETIME_FORMATTER) : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateTossAdConfig(String position, AdminDashboardDto.UpdateTossAdConfigRequest request) {
        AdWatchLocation loc = AdWatchLocation.valueOf(position);
        AdPositionConfig config = adPositionConfigRepository.findByPosition(loc)
                .orElseGet(() -> {
                    AdPositionConfig newConfig = new AdPositionConfig();
                    newConfig.setPosition(loc);
                    return newConfig;
                });

        if (request.getTossAdRatio() != null) config.setTossAdRatio(request.getTossAdRatio());
        if (request.getTossAdGroupId() != null) config.setTossAdGroupId(request.getTossAdGroupId());
        if (request.getIsTossAdEnabled() != null) config.setIsTossAdEnabled(request.getIsTossAdEnabled());
        if (request.getTossImageAdGroupId() != null) config.setTossImageAdGroupId(request.getTossImageAdGroupId());
        if (request.getTossImageAdRatio() != null) config.setTossImageAdRatio(request.getTossImageAdRatio());
        if (request.getIsTossImageAdEnabled() != null) config.setIsTossImageAdEnabled(request.getIsTossImageAdEnabled());
        if (request.getTossBannerAdGroupId() != null) config.setTossBannerAdGroupId(request.getTossBannerAdGroupId());
        if (request.getTossBannerAdRatio() != null) config.setTossBannerAdRatio(request.getTossBannerAdRatio());
        if (request.getIsTossBannerAdEnabled() != null) config.setIsTossBannerAdEnabled(request.getIsTossBannerAdEnabled());
        if (request.getTossInterstitialAdGroupId() != null) config.setTossInterstitialAdGroupId(request.getTossInterstitialAdGroupId());
        if (request.getIsTossInterstitialAdEnabled() != null) config.setIsTossInterstitialAdEnabled(request.getIsTossInterstitialAdEnabled());
        if (request.getRewardedAdRatio() != null) config.setRewardedAdRatio(request.getRewardedAdRatio());
        if (request.getRewardedAdGrams() != null) config.setRewardedAdGrams(request.getRewardedAdGrams());
        if (request.getInterstitialAdGrams() != null) config.setInterstitialAdGrams(request.getInterstitialAdGrams());

        adPositionConfigRepository.save(config);
    }
}
