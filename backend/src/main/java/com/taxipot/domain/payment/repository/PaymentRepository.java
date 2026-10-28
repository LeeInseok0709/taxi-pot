package com.taxipot.domain.payment.repository;

import com.taxipot.domain.payment.entity.Payment;
import com.taxipot.global.code.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findAllByMemberId(Long memberId, Pageable pageable);

    Page<Payment> findAllByMemberIdAndStatus(Long memberId, PaymentStatus status, Pageable pageable);

    List<Payment> findAllByMemberIdAndStatus(Long memberId, PaymentStatus status);
}
