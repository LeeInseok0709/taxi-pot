package com.taxipot.domain.user.service;

import com.taxipot.domain.user.dto.WithdrawRequest;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawFacade {

    /**
     * <b>POST /api/users/me/withdraw</b> — 회원 탈퇴 (논리 삭제)
     * <p>
     * 비밀번호 확인이 필요해 Body를 받으므로 DELETE가 아닌 POST로 설계 (3.2.0에서 `DELETE /api/users/me` → 변경).
     * status = WITHDRAWN, withdrawn_at = now, fcm_token = NULL, token_version + 1. 회원 행은 이용·정산 기록 보존용으로 남긴다.
     * - 검증 순서: 비밀번호 → agreeBalanceForfeit = true(아니면 400 BALANCE_FORFEIT_NOT_AGREED) → 진행 중인 참여·입장 요청·HELD 정산(409 ACTIVE_POT_EXISTS)
     *   → 미수금(UNPAID·OVERDUE, 409 UNPAID_DEBT_EXISTS — 이용 제한 인터셉터에서 먼저 막힘) → 결제 대기 충전 정리
     * - 결제 대기 충전 정리 (3.5.0, PORTONE 연동 시에만 해당 — 시연에서는 PENDING 결제가 생기지 않음): PAYMENT status = PENDING 건마다 포트원 결제 조회 → 결제 완료 상태면 PG 결제 취소, 어느 경우든 status = FAILED (포인트 지급 없음)
     *   - PG 조회·취소가 실패하면 탈퇴를 진행하지 않고 409 PAYMENT_CHECK_FAILED (앱은 잠시 후 재시도 안내)
     * - 앱은 탈퇴 화면에서 잔액이 있으면 '출금 후 탈퇴'를 먼저 안내 (`POST /api/wallet/withdrawals`, 3.5.0)
     * - 남은 지갑 잔액은 소멸 (3.3.0, 약관 명시): balance > 0이면 POINT_HISTORY BALANCE_EXPIRE(−balance), balance = 0
     *   - 앱은 탈퇴 확인 화면에 현재 잔액과 '남은 잔액은 소멸되며 환불되지 않습니다' 문구 + 동의 체크박스를 보여준다
     * - token_version + 1로 이 기기를 포함한 모든 기기의 토큰이 즉시 무효 (이후 요청은 401 TOKEN_REVOKED)
     * - 개인정보는 14일 후 배치(스케줄러 6번)로 파기
     * - 성공 후 앱은 저장된 토큰을 삭제하고 로그인 화면으로 이동
     * <p>
     * <b>에러 코드</b>
     * - 400: BALANCE_FORFEIT_NOT_AGREED
     * - 401: INVALID_CREDENTIALS (비밀번호 불일치)
     * - 409: ACTIVE_POT_EXISTS / UNPAID_DEBT_EXISTS / PAYMENT_CHECK_FAILED
     */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/users/me/withdraw");
    }
}
