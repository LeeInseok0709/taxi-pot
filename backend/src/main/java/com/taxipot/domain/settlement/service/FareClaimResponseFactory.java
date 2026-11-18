package com.taxipot.domain.settlement.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Taxipot → FareClaimResponse 변환. 담당: 지갑·정산
 * <p>운영자 청구 목록, 청구 심사(승인·반려), 방장 실요금 청구가 같은 응답을 쓰므로 한 곳에 둔다.
 */
@Component
@RequiredArgsConstructor
public class FareClaimResponseFactory {

    private static final List<PotMemberStatus> BOARDED = List.of(PotMemberStatus.BOARDED);

    private final TaxipotMemberRepository taxipotMemberRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    /**
     * @param receiptUrlPrefix 영수증 조회 경로 앞부분 — 운영자 화면은 "/api/admin/pots/", 방장 화면은 "/api/pots/"
     */
    public FareClaimResponse of(Taxipot pot, String receiptUrlPrefix) {
        String hostNickname = userRepository.findById(pot.getHostId()).map(User::getNickname).orElse(null);
        int boardedCount = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(pot.getId(), BOARDED).size();

        LocalDateTime startedAt = pot.getStartedAt();
        LocalDateTime claimDeadline = startedAt == null ? null : startedAt.plusHours(props.fareClaim().deadlineHours());

        return new FareClaimResponse(
                pot.getId(),
                pot.getTitle(),
                pot.getHostId(),
                hostNickname,
                pot.getStatus(),
                pot.getFareClaimStatus(),
                pot.getExpectedFare(),
                pot.getActualFare(),
                fareRatio(pot.getActualFare(), pot.getExpectedFare()),
                boardedCount,
                pot.getReceiptImgPath() == null ? null : receiptUrlPrefix + pot.getId() + "/receipt",
                Kst.toOffset(startedAt),
                Kst.toOffset(claimDeadline),
                Kst.toOffset(pot.getFareClaimedAt()),
                Kst.toOffset(pot.getFareReviewedAt()),
                pot.getFareRejectReason());
    }

    /** 실요금 ÷ 예상 요금, 소수 둘째 자리 반올림 (청구 전이거나 예상 요금이 0이면 null) */
    static Double fareRatio(Integer actualFare, Integer expectedFare) {
        if (actualFare == null || expectedFare == null || expectedFare == 0) {
            return null;
        }
        return BigDecimal.valueOf(actualFare)
                .divide(BigDecimal.valueOf(expectedFare), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
