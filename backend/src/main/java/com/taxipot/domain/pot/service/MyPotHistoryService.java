package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.rating.repository.RatingRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.dto.MyPotHistoryPageResponse;
import com.taxipot.domain.user.dto.MyPotHistoryResponse;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class MyPotHistoryService {

    private final TaxipotMemberRepository memberRepository;
    private final TaxipotRepository taxipotRepository;
    private final SettlementRepository settlementRepository;
    private final RatingRepository ratingRepository;

    /**
     * <b>GET /api/users/me/pots</b> — 내 택시 이용 기록 조회
     * <p>
     * 내가 방장이거나 참여한 택시팟 목록 (TAXIPOT_MEMBER 기준, 최신순).
     * 방 정보 + 내 참여 상태 + 내 정산 결과(SETTLEMENT)를 한 번에 반환하므로 '이용 기록'·'정산 내역' 화면에 함께 사용.
     * - 입장 요청만 하고 참여하지 못한 행(REQUESTED·REJECTED, 또는 SETTLEMENT가 없는 행)은 제외 (3.3.0)
     */
    @Transactional(readOnly = true)
    public MyPotHistoryPageResponse getMyPots(Long userId, PotMemberRole role, PotStatus status, int page, int size) {
        return history(userId, role, status, page, size);
    }

    /**
     * <b>GET /api/admin/users/{userId}/pots</b> — 회원 이용 기록 조회
     * <p>
     * 앱의 '내 택시 이용 기록'과 같은 응답 형식
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
     */
    @Transactional(readOnly = true)
    public MyPotHistoryPageResponse getUserPotsForAdmin(Long adminId, Long userId, int page, int size) {
        return history(userId, null, null, page, size);
    }

    private MyPotHistoryPageResponse history(Long memberId, PotMemberRole role, PotStatus status, int page, int size) {
        // 정렬은 쿼리에 고정 (최신 참여순) — 여러 테이블을 묶은 쿼리라 Sort 를 넘기지 않는다
        Page<TaxipotMember> rows = memberRepository.findHistory(memberId, role, status,
                Paging.of(page, size, Sort.unsorted()));
        List<Long> potIds = rows.getContent().stream().map(TaxipotMember::getPotId).toList();

        Map<Long, Taxipot> pots = taxipotRepository.findAllById(potIds).stream()
                .collect(Collectors.toMap(Taxipot::getId, Function.identity()));
        Map<Long, Settlement> settlements = potIds.stream()
                .map(potId -> settlementRepository.findByPotIdAndMemberId(potId, memberId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Settlement::getPotId, Function.identity()));

        List<MyPotHistoryResponse> content = rows.getContent().stream()
                .map(m -> toResponse(m, pots.get(m.getPotId()), settlements.get(m.getPotId())))
                .toList();
        return new MyPotHistoryPageResponse(content, PageMeta.from(rows));
    }

    private MyPotHistoryResponse toResponse(TaxipotMember m, Taxipot pot, Settlement s) {
        boolean isRated = !ratingRepository.findAllByPotIdAndReviewerId(m.getPotId(), m.getMemberId()).isEmpty();
        return new MyPotHistoryResponse(
                pot.getId(),
                pot.getTitle(),
                pot.getDepartureName(),
                pot.getDestinationName(),
                Kst.toOffset(pot.getDepartureTime()),
                pot.getStatus(),
                pot.getActualFare(),
                m.getRole(),
                m.getStatus(),
                s.getHeldAmount(),
                s.getFareShare(),
                s.getFeeAmount(),
                s.getDiffAmount(),
                s.getUncoveredAmount(),
                s.getDebtStatus(),
                s.getDebtAmount(),
                s.getStatus(),
                isRated);
    }
}
