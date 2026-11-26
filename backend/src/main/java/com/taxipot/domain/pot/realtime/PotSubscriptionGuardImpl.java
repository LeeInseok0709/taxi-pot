package com.taxipot.domain.pot.realtime;

import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.security.PotSubscriptionGuard;
import com.taxipot.global.web.UnpaidDebtQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 구독 허용: 그 방의 JOINED·BOARDED 참여자 + 미수금 없음 (3.8.0: STOMP 구독도 이용 제한 대상) */
@Component
@RequiredArgsConstructor
public class PotSubscriptionGuardImpl implements PotSubscriptionGuard {

    private static final List<PotMemberStatus> SUBSCRIBABLE = List.of(PotMemberStatus.JOINED, PotMemberStatus.BOARDED);

    private final TaxipotMemberRepository memberRepository;
    private final UnpaidDebtQuery unpaidDebtQuery;

    @Override
    @Transactional(readOnly = true)
    public void checkSubscribe(Long userId, Long potId) {
        if (!memberRepository.existsByPotIdAndMemberIdAndStatusIn(potId, userId, SUBSCRIBABLE)) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }
        unpaidDebtQuery.findUnpaid(userId).ifPresent(d -> {
            throw Errors.unpaidDebtExists(d.totalAmount(), d.earliestDueAt());
        });
    }
}
