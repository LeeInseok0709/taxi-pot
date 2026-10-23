package com.taxipot.wallet.service;

import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.common.PageResponse;
import com.taxipot.wallet.dto.PaymentConfirmRequest;
import com.taxipot.wallet.dto.PaymentCreateRequest;
import com.taxipot.wallet.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentController 전용 API 서비스. 담당: 효우
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    /**
     * POST /api/payments — 포인트 충전 요청 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 서버가 order_id를 발급하고 PAYMENT 행을 생성한다. 1회 충전 1,000 ~ 500,000원, 충전 후 잔액(balance + held_balance)이 1,000,000원을 넘으면 409.
     * - 앱 충전은 PORTONE만 허용 (16일 회의). PENDING으로 생성 후 반환된 orderId로 앱에서 결제창 호출 → 결제 후 `/api/payments/confirm` 호출
     * - pgProvider = DUMMY는 400 DUMMY_CHARGE_NOT_ALLOWED (3.3.0). 시연 충전은 운영자 웹의 지갑 잔액 조정(`PATCH /api/admin/users/{userId}/wallet`)으로 한다
     * - 3.7.0: PORTONE 연동은 캡스톤 범위 외 — 이 API와 `/api/payments/confirm`은 명세만 유지(구현 선택). 시연 충전은 운영자 잔액 조정으로만 한다
     */
    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/payments");
    }

    /**
     * GET /api/payments — 내 충전 결제 내역 조회 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<PaymentResponse> getPaymentPage(Long userId, PaymentStatus status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/payments");
    }

    /**
     * POST /api/payments/confirm — 충전 결제 승인 (PORTONE 전용) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * PG 결제 완료 후 호출. 서버가 PG사에 결제 금액을 재검증한 뒤 처리한다.
     * - 성공: status = PAID, pg_tid·paid_at 저장, WALLET.balance 증가, POINT_HISTORY CHARGE(payment_id 연결), PAYMENT 알림
     * - 미수금 자동 납부 (3.5.0): CHARGE 직후 같은 트랜잭션에서 미수금을 debt_due_at 빠른 순으로 DEBT_PAY. 응답 balanceAfter는 자동 납부 후 잔액
     *   - 잔액 상한(1,000,000원) 검사는 자동 납부 전 금액 기준
     * - 충전 후 지갑 잔액이 상한(1,000,000원)을 넘으면 PG 결제를 취소하고 status = FAILED (WALLET_LIMIT_EXCEEDED)
     * - 금액 불일치·PG 실패: status = FAILED
     */
    @Transactional
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/payments/confirm");
    }
}
