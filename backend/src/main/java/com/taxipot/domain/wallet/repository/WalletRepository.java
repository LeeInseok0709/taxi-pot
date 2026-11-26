package com.taxipot.domain.wallet.repository;

import com.taxipot.domain.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByMemberId(Long memberId);

    /** 락 순서 2단계: WALLET (USERS 를 먼저 잠근 뒤). 직접 쓰지 말고 MemberWalletLocker 사용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.memberId = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.memberId in :memberIds order by w.memberId asc")
    List<Wallet> findAllByMemberIdInForUpdate(@Param("memberIds") Collection<Long> memberIds);
}
