package com.taxipot.wallet.repository;

import com.taxipot.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 조회 메서드 추가는 wallet 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByMemberId(Long memberId);

    /** 락 순서 2번 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select w from Wallet w where w.memberId = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    /** 여러 회원: member_id 오름차순 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select w from Wallet w where w.memberId in :memberIds order by w.memberId")
    List<Wallet> findAllByMemberIdInForUpdate(@Param("memberIds") Collection<Long> memberIds);
}
