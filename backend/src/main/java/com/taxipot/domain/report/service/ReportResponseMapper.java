package com.taxipot.domain.report.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.report.dto.ReportResponse;
import com.taxipot.domain.report.entity.Report;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 담당: 회원·운영(신고 — 인석 구현)
 * <p>REPORT 행 → 응답 DTO 변환. 신고자·피신고자 닉네임과 방 제목을 신고마다 따로 조회하지 않고 한 번에 모아서 조회한다.
 * <p>신고 서비스 두 개(ReportService, AdminReportService)와 운영자 회원 상세(AdminUserDetailFacade)가 같이 쓴다.
 * 서비스가 아니라 Repository 만 쓰는 변환기라서 어느 쪽에서 주입해도 순환 의존이 생기지 않는다 (CONVENTIONS 5장).
 */
@Component
@RequiredArgsConstructor
public class ReportResponseMapper {

    private final UserRepository userRepository;
    private final TaxipotRepository taxipotRepository;

    /** 내 신고 내역용 (신고자 = 나). 피신고자 닉네임만 필요하다 */
    public List<ReportResponse> toResponses(List<Report> reports) {
        Map<Long, User> users = usersOf(reports);
        return reports.stream()
                .map(r -> new ReportResponse(
                        r.getId(),
                        r.getReportedId(),
                        nickname(users, r.getReportedId()),
                        r.getPotId(),
                        r.getType(),
                        r.getReason(),
                        r.getStatus(),
                        Kst.toOffset(r.getProcessedAt()),
                        Kst.toOffset(r.getCreatedAt())))
                .toList();
    }

    /** 운영자용: 신고자·피신고자 닉네임, 피신고자의 현재 신고 누적 횟수, 방 제목까지 */
    public List<AdminReportResponse> toAdminResponses(List<Report> reports) {
        Map<Long, User> users = usersOf(reports);
        Map<Long, Taxipot> pots = taxipotRepository.findAllById(
                        reports.stream().map(Report::getPotId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Taxipot::getId, Function.identity()));
        return reports.stream()
                .map(r -> {
                    User reported = users.get(r.getReportedId());
                    Taxipot pot = pots.get(r.getPotId());
                    return new AdminReportResponse(
                            r.getId(),
                            r.getReporterId(),
                            nickname(users, r.getReporterId()),
                            r.getReportedId(),
                            nickname(users, r.getReportedId()),
                            reported == null ? null : reported.getReportCount(),
                            r.getPotId(),
                            pot == null ? null : pot.getTitle(),
                            r.getType(),
                            r.getReason(),
                            r.getStatus(),
                            Kst.toOffset(r.getProcessedAt()),
                            Kst.toOffset(r.getCreatedAt()));
                })
                .toList();
    }

    public AdminReportResponse toAdminResponse(Report report) {
        return toAdminResponses(List.of(report)).get(0);
    }

    /** 신고에 나오는 회원(신고자·피신고자)을 한 번에 조회 */
    private Map<Long, User> usersOf(List<Report> reports) {
        Set<Long> ids = new HashSet<>();
        for (Report r : reports) {
            ids.add(r.getReporterId());
            ids.add(r.getReportedId());
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** 탈퇴 후 파기된 회원은 닉네임이 '탈퇴회원_{id}' 로 남아 있으므로 그대로 보여 준다 */
    private static String nickname(Map<Long, User> users, Long id) {
        User u = users.get(id);
        return u == null ? null : u.getNickname();
    }
}
