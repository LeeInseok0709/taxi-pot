package com.taxipot.domain.user.service;

import com.taxipot.domain.pot.service.MyPotHistoryService;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.report.service.AdminReportService;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.dto.AdminUserDetailResponse;
import com.taxipot.domain.user.dto.AdminUserResponse;
import com.taxipot.domain.user.dto.MyPotHistoryResponse;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.verification.dto.AdminVerificationResponse;
import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.domain.verification.repository.StudentVerificationRepository;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 담당: 회원·운영 — 운영자 회원 상세 (인석 구현)
 * <p><b>GET /api/admin/users/{userId}</b> — 회원 상세 조회 (개인정보·지갑·캡처본·신고 이력 포함)
 * <p>
 * <b>에러 코드</b>
 * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
 * - 404: NOT_FOUND
 * <p>왜 Facade 인가: 회원(user) 서비스는 notification 만 부를 수 있는데(CONVENTIONS 5장), 상세 화면은
 * 신고(report)·이용 기록(pot) 서비스 결과까지 모아야 한다. 그래서 '전부 호출 가능'한 Facade 로 두었다 (UserWithdrawFacade 와 같은 방식).
 * 지갑·미수금·인증 이력은 Repository 를 직접 읽는다.
 */
@Component
@RequiredArgsConstructor
public class AdminUserDetailFacade {

    /** 받은 신고는 최근 것부터 이 개수까지만 (명세에 개수 제한이 없어 페이지 최대 크기로 정함) */
    private static final int RECEIVED_REPORTS_LIMIT = 100;
    /** 최근 이용 기록 개수 (명세: 10건, 전체는 /api/admin/users/{userId}/pots) */
    private static final int POT_HISTORY_LIMIT = 10;
    private static final List<DebtStatus> DEBT_STATUSES = List.of(DebtStatus.UNPAID, DebtStatus.OVERDUE);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SettlementRepository settlementRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final AdminReportService adminReportService;
    private final MyPotHistoryService myPotHistoryService;

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long adminId, Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원"));
        AdminUserResponse base = AdminUserService.toResponse(u);

        // 지갑: 운영자 계정 등 지갑이 없는 회원이면 0
        Wallet wallet = walletRepository.findByMemberId(userId).orElse(null);
        int balance = wallet == null ? 0 : wallet.getBalance();
        int held = wallet == null ? 0 : wallet.getHeldBalance();

        // 남은 미수금 합계: 지갑 조회 화면(WalletResponseFactory)과 같은 계산
        int debt = settlementRepository
                .findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(userId, DEBT_STATUSES).stream()
                .filter(Settlement::hasDebt)
                .mapToInt(Settlement::getDebtAmount)
                .sum();

        // 학생 인증 이력 (최근 제출순). 탈퇴 14일 후 스케줄러 6이 삭제하므로 그 뒤엔 빈 목록
        List<AdminVerificationResponse> verifications = studentVerificationRepository.findAllByMemberId(userId).stream()
                .sorted(Comparator.comparing(StudentVerification::getId).reversed())
                .map(v -> toVerificationResponse(v, u))
                .toList();

        List<AdminReportResponse> receivedReports = adminReportService.getReceivedReports(userId, RECEIVED_REPORTS_LIMIT);
        List<MyPotHistoryResponse> potHistory =
                myPotHistoryService.getUserPotsForAdmin(adminId, userId, 0, POT_HISTORY_LIMIT).content();

        return new AdminUserDetailResponse(
                base.id(), base.email(), base.name(), base.nickname(), base.university(), base.studentNo(),
                base.isVerified(), base.ratingAvg(), base.noShowCount(), base.reportCount(), base.role(),
                base.status(), base.suspendedUntil(), base.withdrawnAt(), base.createdAt(),
                u.getCleanStreak(),
                balance,
                held,
                debt,
                verifications,
                receivedReports,
                u.getPhone(),
                u.getGender(),
                potHistory);
    }

    /**
     * StudentVerification → 운영자용 인증 응답.
     * <p>AdminVerificationService(기성, gg/user-profile)의 toResponse 와 같은 모양으로 맞췄다. 그쪽이 머지된 뒤
     * 공용 변환기로 합치면 이 메서드는 지워도 된다. 캡처 이미지는 파일 경로 대신 운영자 전용 조회 API 주소로 준다.
     */
    private static AdminVerificationResponse toVerificationResponse(StudentVerification v, User user) {
        String captureUrl = v.getCaptureImgPath() == null
                ? null : "/api/admin/verifications/" + v.getId() + "/capture";
        return new AdminVerificationResponse(
                v.getId(),
                v.getMemberId(),
                user.getName(),
                user.getGender(),
                user.getUniversity(),
                user.getStudentNo(),
                v.getStudentNo(),
                user.getEmail(),
                v.getEmailStatus(),
                captureUrl,
                v.getReviewStatus(),
                v.getRejectReason(),
                Kst.toOffset(v.getReviewedAt()),
                Kst.toOffset(v.getCreatedAt()));
    }
}
