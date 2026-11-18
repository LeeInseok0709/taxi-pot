package com.taxipot.domain.settlement.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.PotSettlementResponse;
import com.taxipot.domain.settlement.dto.SettlementResponse;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.file.StoredFile;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class SettlementQueryService {

    private static final List<PotMemberStatus> BOARDED = List.of(PotMemberStatus.BOARDED);

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    /**
     * <b>GET /api/pots/{potId}/settlement</b> — 택시팟 정산 내역 조회
     * <p>
     * 해당 방 참여자만 조회 가능. 참여자별 예치액·분담금·수수료·차액·미수금 + 방장 정산금·영수증 URL 반환. 운영자 승인 전에는 정산 값이 null (3.5.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     */
    @Transactional(readOnly = true)
    public PotSettlementResponse getPotSettlement(Long userId, Long potId) {
        // 1. 이 방에 참여한 적이 있는 회원만 (취소·퇴장한 사람도 자기 환불 내역은 볼 수 있게 참여 기록이 있으면 허용)
        if (taxipotMemberRepository.findByPotIdAndMemberId(potId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));

        // 2. 참여자별 정산 행 + 닉네임 (한 번에 조회)
        List<Settlement> settlements = settlementRepository.findAllByPotId(potId).stream()
                .sorted(Comparator.comparing(Settlement::getId))
                .toList();
        Map<Long, String> nicknames = userRepository.findAllById(
                        settlements.stream().map(Settlement::getMemberId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        int boardedCount = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, BOARDED).size();

        // 3. 방장 정산금은 운영자가 승인(APPROVED)해서 실제로 입금된 뒤에만 보여준다
        Integer hostPayout = pot.getFareClaimStatus() == FareClaimStatus.APPROVED ? pot.getActualFare() : null;

        return new PotSettlementResponse(
                pot.getId(),
                pot.getExpectedFare(),
                pot.getActualFare(),
                boardedCount,
                hostPayout,
                pot.getReceiptImgPath() == null ? null : "/api/pots/" + potId + "/receipt",
                settlements.stream().map(s -> toResponse(s, nicknames.get(s.getMemberId()))).toList());
    }

    /**
     * <b>GET /api/pots/{potId}/receipt</b> — 결제내역(영수증) 사진 조회 (3.3.0)
     * <p>
     * 해당 방 탑승자(BOARDED)만 조회 가능 (운영자는 `/api/admin/pots/{potId}/receipt`). 이미지 바이너리 반환, Bearer 토큰 필요
     * (공통 규칙 '이미지 조회 API' 참고). 영수증이 없거나 보관 기간(정산 승인 후 90일)이 지나 파기됐으면 404 (3.6.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public StoredFile getReceipt(Long userId, Long potId) {
        // 1. 끝까지 함께 탄 사람(BOARDED)만 볼 수 있다
        if (!taxipotMemberRepository.existsByPotIdAndMemberIdAndStatusIn(potId, userId, BOARDED)) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        // 2. 영수증이 없거나(장애 복구 입력 등) 90일 지나 파기됐으면 FileStorage 가 404
        return fileStorage.load(pot.getReceiptImgPath());
    }

    private static SettlementResponse toResponse(Settlement s, String nickname) {
        return new SettlementResponse(
                s.getId(),
                s.getPotId(),
                s.getMemberId(),
                nickname,
                s.getHeldAmount(),
                s.getFareShare(),          // 정산 전·환불 행은 null
                s.getFeeAmount(),
                s.getDiffAmount(),
                s.getUncoveredAmount(),
                s.getDebtStatus(),
                s.getDebtAmount(),
                Kst.toOffset(s.getDebtDueAt()),
                s.getStatus(),
                Kst.toOffset(s.getSettledAt()),
                Kst.toOffset(s.getCreatedAt()));
    }
}
