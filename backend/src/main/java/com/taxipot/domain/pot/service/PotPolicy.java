package com.taxipot.domain.pot.service;

import com.taxipot.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 택시팟 금액·시각 계산 규칙 (명세 핵심 정책). 숫자는 모두 AppProperties */
@Component
@RequiredArgsConstructor
public class PotPolicy {

    private final AppProperties props;

    /**
     * 1인 예치액 = floor(expected_fare × 1.2 ÷ max_members) + 수수료 1,000.
     * 정수 계산: expected × 12 ÷ (max × 10). 예) 20,000원·4인 → 6,000 + 1,000 = 7,000
     */
    public int holdAmount(int expectedFare, int maxMembers) {
        AppProperties.Pot p = props.pot();
        long share = (long) expectedFare * p.holdBufferNumerator() / ((long) maxMembers * p.holdBufferDenominator());
        return (int) share + p.feeAmount();
    }

    public int feeAmount() {
        return props.pot().feeAmount();
    }

    /** 집결 기한 = 참여 마감 = 출발 5분 전 */
    public LocalDateTime arrivalDeadline(LocalDateTime departureTime) {
        return departureTime.minusMinutes(props.pot().arrivalDeadlineMinutes());
    }

    /** 이 시각부터 참여 취소·방 취소 불가 (출발 10분 전) */
    public LocalDateTime cancelDeadline(LocalDateTime departureTime) {
        return departureTime.minusMinutes(props.pot().cancelDeadlineMinutes());
    }

    /** CANCEL_TIME_EXPIRED 문구용: Errors.cancelTimeExpired(potPolicy.cancelDeadline(dep), potPolicy.cancelDeadlineMinutes()) */
    public long cancelDeadlineMinutes() {
        return props.pot().cancelDeadlineMinutes();
    }

    public long minLeadMinutes() {
        return props.pot().minLeadMinutes();
    }

    public long maxAdvanceHours() {
        return props.pot().maxAdvanceHours();
    }

    public LocalDateTime minDepartureTime(LocalDateTime now) {
        return now.plusMinutes(props.pot().minLeadMinutes());
    }

    public LocalDateTime maxDepartureTime(LocalDateTime now) {
        return now.plusHours(props.pot().maxAdvanceHours());
    }

    public int arrivalRadiusMeters() {
        return props.arrival().radiusMeters();
    }
}
