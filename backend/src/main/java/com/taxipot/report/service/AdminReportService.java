package com.taxipot.report.service;

import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.common.PageResponse;
import com.taxipot.report.dto.AdminReportResponse;
import com.taxipot.report.dto.ReportProcessRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminReportController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    /**
     * GET /api/admin/reports — 신고 목록 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<AdminReportResponse> getReportPage(Long adminId, ReportStatus status, ReportType type, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/admin/reports");
    }

    /**
     * PATCH /api/admin/reports/{reportId} — 신고 처리 (승인·반려) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * - APPROVED: 피신고자 report_count + 1, clean_streak = 0
     *   - 누적 3회: 3일 정지 / 6회: 7일 정지 / 10회: 영구 정지(suspended_until = NULL)
     *   - 정지 규칙 통일 (3.3.0): 이미 정지 중이면 기존 정지와 비교해 더 긴 쪽 유지 (예: 15분 노쇼 정지 중 3일 정지 → 3일, 영구 정지 중 → 영구 유지)
     *   - 신고 유형이 NO_SHOW여도 no_show_count·참여 상태는 바꾸지 않음 (노쇼 집계는 미도착 퇴장 시점에만, 이중 집계 방지)
     * - REJECTED: 변경 없음
     * - 공통: processed_at = now, 신고자·피신고자에게 REPORT_RESULT 알림
     */
    @Transactional
    public AdminReportResponse processReport(Long adminId, Long reportId, ReportProcessRequest request) {
        throw new UnsupportedOperationException("TODO(서연): PATCH /api/admin/reports/{reportId}");
    }
}
