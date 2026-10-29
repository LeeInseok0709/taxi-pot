package com.taxipot.domain.wallet.service;

import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 명세 '동시성 제어' 락 순서를 강제하는 유일한 진입점: USERS → WALLET → (호출 측에서) TAXIPOT.
 * <pre>
 * &#64;Transactional
 * public JoinResponse join(Long userId, Long potId, JoinRequest req) {
 *     LockedMember me = locker.lock(userId);                       // 1. USERS, 2. WALLET
 *     Taxipot pot = taxipotRepository.findByIdForUpdate(potId)...; // 3. TAXIPOT
 *     ...
 * }
 * </pre>
 * 여러 회원(정산·방 취소·스케줄러)은 lockAll — member_id 오름차순으로 USERS 전부 → WALLET 전부.
 * 방장이 다른 회원을 처리(내보내기·입장 요청 수락)할 때는 '대상 회원' id 로 lock 한다.
 */
@Component
@RequiredArgsConstructor
public class MemberWalletLocker {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /** 반드시 @Transactional 안에서 호출 */
    @Transactional(propagation = Propagation.MANDATORY)
    public LockedMember lock(Long memberId) {
        User user = userRepository.findByIdForUpdate(memberId).orElseThrow(() -> Errors.notFound("회원"));
        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId).orElseThrow(() -> Errors.notFound("지갑"));
        return new LockedMember(user, wallet);
    }

    /** member_id 오름차순으로 잠근 결과 (키 순서도 오름차순) */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, LockedMember> lockAll(Collection<Long> memberIds) {
        List<Long> sorted = List.copyOf(new TreeSet<>(memberIds));
        Map<Long, User> users = new LinkedHashMap<>();
        userRepository.findAllByIdInForUpdate(sorted).forEach(u -> users.put(u.getId(), u));
        Map<Long, Wallet> wallets = new LinkedHashMap<>();
        walletRepository.findAllByMemberIdInForUpdate(sorted).forEach(w -> wallets.put(w.getMemberId(), w));
        Map<Long, LockedMember> result = new LinkedHashMap<>();
        for (Long id : sorted) {
            User u = users.get(id);
            Wallet w = wallets.get(id);
            if (u == null || w == null) {
                throw Errors.notFound("회원·지갑(" + id + ")");
            }
            result.put(id, new LockedMember(u, w));
        }
        return result;
    }
}
