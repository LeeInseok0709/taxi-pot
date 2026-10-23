package com.taxipot.pot.service;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.pot.entity.Taxipot;
import com.taxipot.pot.repository.TaxipotMemberRepository;
import com.taxipot.pot.repository.TaxipotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 택시팟 공용 기능 (Manager). 다른 도메인(탈퇴·평가·신고·채팅·정산)이 방 정보를 쓸 때 사용.
 * 주입 가능: UserManager, WalletManager, NotificationManager, Repository. 주입 금지: 모든 API 서비스.
 * 메서드 추가는 서연.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotManager {

    private static final List<PotMemberStatus> ACTIVE_MEMBER =
            List.of(PotMemberStatus.JOINED, PotMemberStatus.BOARDED, PotMemberStatus.REQUESTED);
    private static final List<PotStatus> OPEN_POT = List.of(PotStatus.RECRUITING, PotStatus.CLOSED);

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;

    public Taxipot getPot(Long potId) {
        return taxipotRepository.findById(potId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 락 순서 3번 (TAXIPOT). USERS·WALLET을 먼저 잠근 뒤 호출 */
    @Transactional
    public Taxipot lockPot(Long potId) {
        return taxipotRepository.findByIdForUpdate(potId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 명세 '진행 중인 다른 방'의 방 번호 (ACTIVE_POT_EXISTS detail.activePotId) */
    public Optional<Long> findActivePotId(Long userId) {
        return taxipotMemberRepository.findActivePotIds(userId, ACTIVE_MEMBER, OPEN_POT,
                PotStatus.RUNNING, FareClaimStatus.NONE).stream().findFirst();
    }

    public boolean hasActivePot(Long userId) {
        return findActivePotId(userId).isPresent();
    }
}
