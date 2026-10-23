package com.taxipot.wallet.dto;

import com.taxipot.global.code.WithdrawalStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 WithdrawalResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 출금 1건 (3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record WithdrawalResponse(
        Long id,
        Integer amount,
        /** 탈퇴 14일 후 파기되면 null; null 가능 */
        String bankName,
        /** null 가능 */
        String maskedAccountNumber,
        /** null 가능 */
        String accountHolder,
        /** DUMMY: 시연용 가상 송금 */
        String transferProvider,
        /** null 가능 */
        String transferTid,
        WithdrawalStatus status,
        /** null 가능 */
        String failReason,
        /** 처리 직후 지갑 사용 가능 잔액 (내역 조회에서는 null); null 가능 */
        Integer balanceAfter,
        OffsetDateTime requestedAt,
        /** null 가능 */
        OffsetDateTime completedAt
) {}
