package com.taxipot.report.service;

import com.taxipot.global.common.PageResponse;
import com.taxipot.report.dto.ReportCreateRequest;
import com.taxipot.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReportController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /**
     * GET /api/users/me/reports — 내가 접수한 신고 내역 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<ReportResponse> getMyReportPage(Long userId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/users/me/reports");
    }

    /**
     * POST /api/reports — 신고 접수 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * - 신고 자격 (16일 회의, 서버 검증)
     *   - ① 신고자·피신고자 모두 그 방 BOARDED (끝까지 함께 탑승) — 모든 신고 유형
     *   - ② 피신고자가 그 방 NO_SHOW이고 신고자가 끝까지 남은 참여자(JOINED·BOARDED) — type = NO_SHOW만 (방장 미도착으로 자동 해산된 방의 동승자 포함)
     *   - 그 외(중간 취소 CANCELED, KICKED, 본인 NO_SHOW, REQUESTED·REJECTED, 참여 이력 없음)는 403 REPORT_NOT_ELIGIBLE
     * - 본인 신고 불가, 같은 방·같은 대상 중복 신고 불가 (UNIQUE(pot_id, reporter_id, reported_id))
     * - 운영자 승인(APPROVED) 시에만 report_count 증가
     */
    @Transactional
    public ReportResponse createReport(Long userId, ReportCreateRequest request) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/reports");
    }
}
