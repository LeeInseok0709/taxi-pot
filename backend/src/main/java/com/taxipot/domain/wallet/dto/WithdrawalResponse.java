package com.taxipot.domain.wallet.dto;

import com.taxipot.global.code.WithdrawalStatus;

import java.time.OffsetDateTime;

/**
 * WithdrawalResponse — API 명세서 3.9.0 components.schemas.WithdrawalResponse
 * <p>출금 1건 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record WithdrawalResponse(
        Long id,
        Integer amount,
        // nullable / 탈퇴 14일 후 파기되면 null
        String bankName,
        // nullable
        String maskedAccountNumber,
        // nullable
        String accountHolder,
        // DUMMY: 시연용 가상 송금
        String transferProvider,
        // nullable
        String transferTid,
        WithdrawalStatus status,
        // nullable
        String failReason,
        // nullable / 처리 직후 지갑 사용 가능 잔액 (내역 조회에서는 null)
        Integer balanceAfter,
        OffsetDateTime requestedAt,
        // nullable
        OffsetDateTime completedAt
) {
}
