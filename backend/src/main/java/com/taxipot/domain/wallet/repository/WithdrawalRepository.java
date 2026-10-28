package com.taxipot.domain.wallet.repository;

import com.taxipot.domain.wallet.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findAllByMemberId(Long memberId, Pageable pageable);

    List<Withdrawal> findAllByMemberId(Long memberId);
}
