package com.taxipot.domain.user.service;

import com.taxipot.domain.auth.dto.TokenResponse;
import com.taxipot.domain.user.dto.AccountResponse;
import com.taxipot.domain.user.dto.AccountUpdateRequest;
import com.taxipot.domain.user.dto.FcmTokenRequest;
import com.taxipot.domain.user.dto.MyProfileResponse;
import com.taxipot.domain.user.dto.PasswordChangeRequest;
import com.taxipot.domain.user.dto.UserPublicResponse;
import com.taxipot.domain.user.dto.UserUpdateRequest;
import com.taxipot.domain.user.mapper.UserMapper;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.error.Errors;
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
public class UserService {

    private final UserRepository userRepository;

    /**
     * <b>GET /api/users/me</b> — 내 정보 조회
     * <p>
     * 평점·신고 누적·무신고 연속 횟수·정지 상태를 포함 (정지 안내 화면에도 사용)
     * <p>
     * <b>에러 코드</b>
     * - 401: UNAUTHORIZED
     */
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        return UserMapper.toMyProfile(userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원")));
    }

    /**
     * <b>PATCH /api/users/me</b> — 내 정보 수정
     * <p>
     * 닉네임·휴대폰번호·마케팅 수신 동의만 수정 가능 (이름·성별·학번은 인증 정보라 수정 불가). 변경할 항목만 전달
     * - 닉네임이 '탈퇴회원_'으로 시작하면 400 RESERVED_NICKNAME (3.3.0)
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / RESERVED_NICKNAME
     * - 409: DUPLICATE_NICKNAME
     */
    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/users/me");
    }

    /**
     * <b>PATCH /api/users/me/password</b> — 비밀번호 변경
     * <p>
     * 현재 비밀번호 확인 후 pw_hash 변경, token_version + 1 (3.3.0).
     * - 다른 기기의 기존 토큰은 모두 무효(401 TOKEN_REVOKED). 이 기기는 응답의 새 토큰으로 교체해 로그인 상태를 유지한다
     * - 3.2.0의 204 No Content → 3.3.0부터 200 + TokenResponse (앱 Retrofit 반환 타입을 `Response<Unit>` → `TokenResponse`로 변경)
     * <p>
     * <b>에러 코드</b>
     * - 401: INVALID_CREDENTIALS (현재 비밀번호 불일치)
     */
    @Transactional
    public TokenResponse changePassword(Long userId, PasswordChangeRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/users/me/password");
    }

    /**
     * <b>GET /api/users/me/account</b> — 내 출금 계좌 조회 (3.5.0)
     * <p>
     * 계좌번호는 마스킹해서 반환 (예 123-****-7890). 9차 이전 가입자처럼 미등록이면 404
     * <p>
     * <b>에러 코드</b>
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(Long userId) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/users/me/account");
    }

    /**
     * <b>PUT /api/users/me/account</b> — 출금 계좌 등록·변경 (3.5.0)
     * <p>
     * 현재 비밀번호 확인 후 USERS.bank_code·account_no·account_holder를 한 번에 교체 (본인 명의 1개만).
     * - accountHolder는 USERS.name과 같아야 함 (아니면 400 ACCOUNT_HOLDER_MISMATCH), 계좌번호는 숫자만 남겨 AES-256-GCM 암호화
     * - 이미 신청된 출금(WITHDRAWAL)에는 신청 시점 계좌 스냅샷이 남으므로 영향 없음
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / ACCOUNT_HOLDER_MISMATCH
     * - 401: INVALID_CREDENTIALS (비밀번호 불일치)
     */
    @Transactional
    public AccountResponse updateMyAccount(Long userId, AccountUpdateRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PUT /api/users/me/account");
    }

    /**
     * <b>PUT /api/users/me/fcm-token</b> — FCM 푸시 토큰 등록·갱신
     */
    @Transactional
    public void updateFcmToken(Long userId, FcmTokenRequest request) {
        userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원")).changeFcmToken(request.fcmToken());
    }

    /**
     * <b>GET /api/users/{userId}</b> — 회원 공개 프로필 조회
     * <p>
     * 동승자 확인용. 닉네임·대학교·평점만 공개 (실명·학번 등 개인정보 비공개)
     * <p>
     * <b>에러 코드</b>
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public UserPublicResponse getPublicProfile(Long requesterId, Long userId) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/users/{userId}");
    }
}
