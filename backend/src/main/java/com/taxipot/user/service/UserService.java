package com.taxipot.user.service;

import com.taxipot.user.dto.AccountResponse;
import com.taxipot.user.dto.AccountUpdateRequest;
import com.taxipot.user.dto.FcmTokenRequest;
import com.taxipot.user.dto.MyProfileResponse;
import com.taxipot.user.dto.PasswordChangeRequest;
import com.taxipot.user.dto.TokenResponse;
import com.taxipot.user.dto.UserPublicResponse;
import com.taxipot.user.dto.UserUpdateRequest;
import com.taxipot.user.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    /**
     * GET /api/users/me — 내 정보 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 평점·신고 누적·무신고 연속 횟수·정지 상태를 포함 (정지 안내 화면에도 사용)
     */
    public MyProfileResponse getMyProfile(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/users/me");
    }

    /**
     * PATCH /api/users/me — 내 정보 수정 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 닉네임·휴대폰번호·마케팅 수신 동의만 수정 가능 (이름·성별·학번은 인증 정보라 수정 불가). 변경할 항목만 전달
     * - 닉네임이 '탈퇴회원_'으로 시작하면 400 RESERVED_NICKNAME (3.3.0)
     */
    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/users/me");
    }

    /**
     * POST /api/users/me/withdraw — 회원 탈퇴 (논리 삭제) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
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
     */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/users/me/withdraw");
    }

    /**
     * PATCH /api/users/me/password — 비밀번호 변경 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 현재 비밀번호 확인 후 pw_hash 변경, token_version + 1 (3.3.0).
     * - 다른 기기의 기존 토큰은 모두 무효(401 TOKEN_REVOKED). 이 기기는 응답의 새 토큰으로 교체해 로그인 상태를 유지한다
     * - 3.2.0의 204 No Content → 3.3.0부터 200 + TokenResponse (앱 Retrofit 반환 타입을 `Response<Unit>` → `TokenResponse`로 변경)
     */
    @Transactional
    public TokenResponse changePassword(Long userId, PasswordChangeRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/users/me/password");
    }

    /**
     * GET /api/users/me/account — 내 출금 계좌 조회 (3.5.0) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 계좌번호는 마스킹해서 반환 (예 123-****-7890). 9차 이전 가입자처럼 미등록이면 404
     */
    public AccountResponse getMyAccount(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/users/me/account");
    }

    /**
     * PUT /api/users/me/account — 출금 계좌 등록·변경 (3.5.0) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 현재 비밀번호 확인 후 USERS.bank_code·account_no·account_holder를 한 번에 교체 (본인 명의 1개만).
     * - accountHolder는 USERS.name과 같아야 함 (아니면 400 ACCOUNT_HOLDER_MISMATCH), 계좌번호는 숫자만 남겨 AES-256-GCM 암호화
     * - 이미 신청된 출금(WITHDRAWAL)에는 신청 시점 계좌 스냅샷이 남으므로 영향 없음
     */
    @Transactional
    public AccountResponse updateMyAccount(Long userId, AccountUpdateRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PUT /api/users/me/account");
    }

    /**
     * PUT /api/users/me/fcm-token — FCM 푸시 토큰 등록·갱신 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    @Transactional
    public void updateFcmToken(Long userId, FcmTokenRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PUT /api/users/me/fcm-token");
    }

    /**
     * GET /api/users/{userId} — 회원 공개 프로필 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 동승자 확인용. 닉네임·대학교·평점만 공개 (실명·학번 등 개인정보 비공개)
     */
    public UserPublicResponse getUserProfile(Long userId, Long targetUserId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/users/{userId}");
    }
}
