package com.taxipot.domain.report.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.report.dto.AdminReportPageResponse;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.report.dto.ReportProcessRequest;
import com.taxipot.domain.report.entity.Report;
import com.taxipot.domain.report.repository.ReportRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 담당: 회원·운영 — 운영자 신고 관리 (인석 구현)
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다. (report → notification 허용)
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    /** 신고 승인 누적 횟수별 정지 (명세: 3회 3일 / 6회 7일 / 10회 영구) */
    private static final int SUSPEND_3_DAYS_AT = 3;
    private static final int SUSPEND_7_DAYS_AT = 6;
    private static final int SUSPEND_FOREVER_AT = 10;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportResponseMapper reportResponseMapper;
    private final NotificationSender notificationSender;
    private final TimeProvider timeProvider;

    /**
     * <b>GET /api/admin/reports</b> — 신고 목록 조회
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
     */
    @Transactional(readOnly = true)
    public AdminReportPageResponse getReports(Long adminId, ReportStatus status, ReportType type, int page, int size) {
        // 구현: 인석 — status·type 은 선택 필터, 최신 접수순
        Page<Report> rows = reportRepository.search(status, type,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new AdminReportPageResponse(reportResponseMapper.toAdminResponses(rows.getContent()), PageMeta.from(rows));
    }

    /**
     * <b>PATCH /api/admin/reports/{reportId}</b> — 신고 처리 (승인·반려)
     * <p>
     * - APPROVED: 피신고자 report_count + 1, clean_streak = 0
     *   - 누적 3회: 3일 정지 / 6회: 7일 정지 / 10회: 영구 정지(suspended_until = NULL)
     *   - 정지 규칙 통일 (3.3.0): 이미 정지 중이면 기존 정지와 비교해 더 긴 쪽 유지 (예: 15분 노쇼 정지 중 3일 정지 → 3일, 영구 정지 중 → 영구 유지)
     *   - 신고 유형이 NO_SHOW여도 no_show_count·참여 상태는 바꾸지 않음 (노쇼 집계는 미도착 퇴장 시점에만, 이중 집계 방지)
     * - REJECTED: 변경 없음
     * - 공통: processed_at = now, 신고자·피신고자에게 REPORT_RESULT 알림
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 409: ALREADY_PROCESSED
     */
    @Transactional
    public AdminReportResponse processReport(Long adminId, Long reportId, ReportProcessRequest request) {
        // 구현: 인석
        ReportStatus decision = request.status();
        if (decision != ReportStatus.APPROVED && decision != ReportStatus.REJECTED) {
            throw Errors.invalidInput("status 는 APPROVED 또는 REJECTED 만 가능합니다.");
        }

        // 1. 잠금 순서: 피신고자 USERS → REPORT (CONVENTIONS 8장). 피신고자 id 를 알기 위해 먼저 락 없이 한 번 읽는다
        Long reportedId = reportRepository.findById(reportId)
                .orElseThrow(() -> Errors.notFound("신고"))
                .getReportedId();
        User reported = userRepository.findByIdForUpdate(reportedId)
                .orElseThrow(() -> Errors.notFound("회원"));
        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> Errors.notFound("신고"));

        // 2. 잠근 뒤 다시 확인: 다른 운영자가 먼저 처리했으면 409 (신고 횟수가 두 번 오르지 않게)
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED);
        }

        LocalDateTime now = timeProvider.now();
        if (decision == ReportStatus.APPROVED) {
            report.approve(now);
            // report_count + 1, clean_streak = 0 (User.increaseReportCount). no_show_count 는 건드리지 않는다
            int count = reported.increaseReportCount();
            LocalDateTime until = suspensionFor(count, now);
            if (count >= SUSPEND_FOREVER_AT) {
                reported.suspend(null);              // 영구 정지
            } else if (until != null) {
                reported.suspend(until);             // 기존 정지가 더 길면 User.suspend 가 기존 값을 남긴다
            }
        } else {
            report.reject(now);                      // 반려: 피신고자 변경 없음
        }

        // 3. 알림: 명세대로 승인·반려 모두 신고자·피신고자 두 사람에게 REPORT_RESULT (문구는 명세에 없어 임의로 정함)
        boolean approved = decision == ReportStatus.APPROVED;
        notificationSender.send(report.getReporterId(), report.getPotId(), NotificationType.REPORT_RESULT,
                "신고 처리 결과", "접수하신 신고가 " + (approved ? "승인" : "반려") + "되었습니다.");
        notificationSender.send(reported.getId(), report.getPotId(), NotificationType.REPORT_RESULT,
                "신고 처리 결과", approved
                        ? "회원님에 대한 신고가 승인되었습니다. 누적 횟수에 따라 이용이 제한될 수 있습니다."
                        : "회원님에 대한 신고가 검토 후 반려되었습니다.");
        return reportResponseMapper.toAdminResponse(report);
    }

    /** 누적 3회 → 3일, 6회 → 7일 정지 시각. 그 외 횟수는 null (10회 이상 영구는 호출 측에서 처리) */
    private static LocalDateTime suspensionFor(int count, LocalDateTime now) {
        if (count == SUSPEND_7_DAYS_AT) {
            return now.plusDays(7);
        }
        if (count == SUSPEND_3_DAYS_AT) {
            return now.plusDays(3);
        }
        return null;
    }

    /** 운영자 회원 상세(AdminUserDetailFacade)에서 쓰는 '받은 신고' 목록 */
    @Transactional(readOnly = true)
    public List<AdminReportResponse> getReceivedReports(Long userId, int limit) {
        Page<Report> rows = reportRepository.findAllByReportedId(userId,
                Paging.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return reportResponseMapper.toAdminResponses(rows.getContent());
    }
}
