package com.taxipot.domain.report.service;

import com.taxipot.domain.report.dto.ReportCreateRequest;
import com.taxipot.domain.report.dto.ReportPageResponse;
import com.taxipot.domain.report.dto.ReportResponse;
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
public class ReportService {

    /**
     * <b>GET /api/users/me/reports</b> — 내가 접수한 신고 내역 조회
     */
    @Transactional(readOnly = true)
    public ReportPageResponse getMyReports(Long userId, int page, int size) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/users/me/reports");
    }

    /**
     * <b>POST /api/reports</b> — 신고 접수
     * <p>
     * - 신고 자격 (16일 회의, 서버 검증)
     *   - ① 신고자·피신고자 모두 그 방 BOARDED (끝까지 함께 탑승) — 모든 신고 유형
     *   - ② 피신고자가 그 방 NO_SHOW이고 신고자가 끝까지 남은 참여자(JOINED·BOARDED) — type = NO_SHOW만 (방장 미도착으로 자동 해산된 방의 동승자 포함)
     *   - 그 외(중간 취소 CANCELED, KICKED, 본인 NO_SHOW, REQUESTED·REJECTED, 참여 이력 없음)는 403 REPORT_NOT_ELIGIBLE
     * - 본인 신고 불가, 같은 방·같은 대상 중복 신고 불가 (UNIQUE(pot_id, reporter_id, reported_id))
     * - 운영자 승인(APPROVED) 시에만 report_count 증가
     * <p>
     * <b>에러 코드</b>
     * - 400: SELF_REPORT_NOT_ALLOWED / INVALID_INPUT(②인데 type이 NO_SHOW가 아님)
     * - 403: REPORT_NOT_ELIGIBLE
     * - 409: DUPLICATE_REPORT
     */
    @Transactional
    public ReportResponse createReport(Long userId, ReportCreateRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/reports");
    }
}
