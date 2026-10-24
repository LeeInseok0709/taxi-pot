package com.taxipot.domain.payment.service;

import com.taxipot.domain.payment.dto.PaymentConfirmRequest;
import com.taxipot.domain.payment.dto.PaymentCreateRequest;
import com.taxipot.domain.payment.dto.PaymentPageResponse;
import com.taxipot.domain.payment.dto.PaymentResponse;
import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    /**
     * <b>POST /api/payments</b> — 포인트 충전 요청
     * <p>
     * 서버가 order_id를 발급하고 PAYMENT 행을 생성한다. 1회 충전 1,000 ~ 500,000원, 충전 후 잔액(balance + held_balance)이 1,000,000원을 넘으면 409.
     * - 앱 충전은 PORTONE만 허용 (16일 회의). PENDING으로 생성 후 반환된 orderId로 앱에서 결제창 호출 → 결제 후 `/api/payments/confirm` 호출
     * - pgProvider = DUMMY는 400 DUMMY_CHARGE_NOT_ALLOWED (3.3.0). 시연 충전은 운영자 웹의 지갑 잔액 조정(`PATCH /api/admin/users/{userId}/wallet`)으로 한다
     * - 3.7.0: PORTONE 연동은 캡스톤 범위 외 — 이 API와 `/api/payments/confirm`은 명세만 유지(구현 선택). 시연 충전은 운영자 잔액 조정으로만 한다
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / DUMMY_CHARGE_NOT_ALLOWED
     * - 409: WALLET_LIMIT_EXCEEDED
     */
    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/payments");
    }

    /**
     * <b>GET /api/payments</b> — 내 충전 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public PaymentPageResponse getMyPayments(Long userId, PaymentStatus status, int page, int size) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/payments");
    }

    /**
     * <b>POST /api/payments/confirm</b> — 충전 결제 승인 (PORTONE 전용)
     * <p>
     * PG 결제 완료 후 호출. 서버가 PG사에 결제 금액을 재검증한 뒤 처리한다.
     * - 성공: status = PAID, pg_tid·paid_at 저장, WALLET.balance 증가, POINT_HISTORY CHARGE(payment_id 연결), PAYMENT 알림
     * - 미수금 자동 납부 (3.5.0): CHARGE 직후 같은 트랜잭션에서 미수금을 debt_due_at 빠른 순으로 DEBT_PAY. 응답 balanceAfter는 자동 납부 후 잔액
     *   - 잔액 상한(1,000,000원) 검사는 자동 납부 전 금액 기준
     * - 충전 후 지갑 잔액이 상한(1,000,000원)을 넘으면 PG 결제를 취소하고 status = FAILED (WALLET_LIMIT_EXCEEDED)
     * - 금액 불일치·PG 실패: status = FAILED
     * <p>
     * <b>에러 코드</b>
     * - 400: PAYMENT_AMOUNT_MISMATCH / PAYMENT_FAILED
     * - 409: PAYMENT_ALREADY_PROCESSED
     */
    @Transactional
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/payments/confirm");
    }
}
