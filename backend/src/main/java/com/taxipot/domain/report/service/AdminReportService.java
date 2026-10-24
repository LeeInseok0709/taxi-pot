package com.taxipot.domain.report.service;

import com.taxipot.domain.report.dto.AdminReportPageResponse;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.report.dto.ReportProcessRequest;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    /**
     * <b>GET /api/admin/reports</b> — 신고 목록 조회
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public AdminReportPageResponse getReports(Long adminId, ReportStatus status, ReportType type, int page, int size) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/reports");
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
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/admin/reports/{reportId}");
    }
}
