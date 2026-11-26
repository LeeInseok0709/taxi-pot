package com.taxipot.domain.report.service;

import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.report.dto.ReportCreateRequest;
import com.taxipot.domain.report.dto.ReportPageResponse;
import com.taxipot.domain.report.dto.ReportResponse;
import com.taxipot.domain.report.entity.Report;
import com.taxipot.domain.report.repository.ReportRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영 — 신고 (인석 구현)
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다. (여기서는 Repository 만 쓴다)
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final ReportResponseMapper reportResponseMapper;

    /**
     * <b>GET /api/users/me/reports</b> — 내가 접수한 신고 내역 조회
     */
    @Transactional(readOnly = true)
    public ReportPageResponse getMyReports(Long userId, int page, int size) {
        // 구현: 인석 — 최신 접수순
        Page<Report> rows = reportRepository.findAllByReporterId(userId,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new ReportPageResponse(reportResponseMapper.toResponses(rows.getContent()), PageMeta.from(rows));
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
        // 구현: 인석
        Long potId = request.potId();
        Long reportedId = request.reportedId();

        // 1. 본인 신고 불가 → 400
        if (userId.equals(reportedId)) {
            throw new BusinessException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        // 2. 신고 자격 (그 방에서의 두 사람 참여 상태로 판단) → 403 / 400
        PotMemberStatus reporter = memberStatus(potId, userId);
        PotMemberStatus reported = memberStatus(potId, reportedId);
        boolean bothBoarded = reporter == PotMemberStatus.BOARDED && reported == PotMemberStatus.BOARDED;   // ①
        boolean noShowCase = reported == PotMemberStatus.NO_SHOW
                && (reporter == PotMemberStatus.JOINED || reporter == PotMemberStatus.BOARDED);           // ②
        if (!bothBoarded && !noShowCase) {
            throw new BusinessException(ErrorCode.REPORT_NOT_ELIGIBLE);
        }
        if (!bothBoarded && request.type() != ReportType.NO_SHOW) {
            // ② 로만 자격이 되는데 유형이 NO_SHOW 가 아님 (노쇼한 사람에게는 노쇼 신고만 가능)
            throw Errors.invalidInput("노쇼한 참여자는 노쇼(NO_SHOW) 유형으로만 신고할 수 있습니다.");
        }

        // 3. 같은 방·같은 대상 중복 → 409. 먼저 확인하고, 동시에 두 번 눌러 UNIQUE 에 걸려도 같은 409 로 바꾼다
        if (reportRepository.existsByPotIdAndReporterIdAndReportedId(potId, userId, reportedId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
        Report saved;
        try {
            saved = reportRepository.saveAndFlush(
                    Report.create(userId, reportedId, potId, request.type(), request.reason().strip()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
        // report_count 는 운영자가 승인할 때만 오른다 (AdminReportService.processReport)
        return reportResponseMapper.toResponses(java.util.List.of(saved)).get(0);
    }

    /** 그 방에서 회원의 참여 상태. 참여 이력이 없으면 null (→ 자격 없음) */
    private PotMemberStatus memberStatus(Long potId, Long memberId) {
        return taxipotMemberRepository.findByPotIdAndMemberId(potId, memberId)
                .map(TaxipotMember::getStatus)
                .orElse(null);
    }
}
