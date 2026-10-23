package com.taxipot.user.service;

import com.taxipot.user.dto.AvailabilityResponse;
import com.taxipot.user.dto.LoginRequest;
import com.taxipot.user.dto.LoginResponse;
import com.taxipot.user.dto.RefreshRequest;
import com.taxipot.user.dto.SignupRequest;
import com.taxipot.user.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /**
     * POST /api/auth/signup — 회원가입 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 하나의 트랜잭션으로 USERS(is_verified = 0), WALLET(잔액 0), STUDENT_VERIFICATION(email_status = PENDING, student_no = 가입 학번) 행을 생성하고
     * 대학교 이메일로 인증코드를 발송한다. 응답으로 토큰을 바로 발급해 이후 이메일 인증 → 캡처본 제출을 이어서 진행한다.
     * - 필수 약관 동의(agreeRequired = true)가 아니면 가입 불가, agreed_at은 서버 시각
     * - 비밀번호는 BCrypt 해시 후 pw_hash에 저장, 휴대폰번호는 정규화 후 AES-256-GCM 암호화해 저장
     * - 같은 학교 + 학번 중복 가입 불가 (탈퇴 14일 후 파기된 학번은 재가입 가능)
     * - 검증 순서 (3.3.0): 입력 형식 → 닉네임 예약어('탈퇴회원_' 시작이면 RESERVED_NICKNAME) → 학교(UNIVERSITY 목록·is_active = 1, 아니면 UNSUPPORTED_UNIVERSITY)
     *   → 이메일 도메인(소문자 비교, '@' 뒤가 학교 email_domain과 정확히 일치해야 함, 아니면 EMAIL_DOMAIN_MISMATCH) → 중복(이메일·닉네임·학번)
     * - 중복 검사는 사전 조회 + DB UNIQUE 위반(DataIntegrityViolationException) 변환을 함께 사용 (동시 가입 대비)
     * - 발급 토큰의 claim tv = 0 (USERS.token_version 초기값)
     * - 출금 계좌 (3.5.0): bankCode는 `GET /api/banks` 목록 코드, accountNumber는 하이픈 제거 후 숫자 10~14자리, accountHolder는 name과 같아야 함(아니면 400 ACCOUNT_HOLDER_MISMATCH)
     *   계좌번호는 AES-256-GCM 암호화 저장. 시연은 실제 예금주 조회 없이 이름 비교만
     */
    @Transactional
    public LoginResponse signup(SignupRequest request) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/auth/signup");
    }

    /**
     * GET /api/auth/nickname-check — 닉네임 사용 가능 여부 확인 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 중복이면 available = false, reason = DUPLICATE. '탈퇴회원_'으로 시작하면 available = false, reason = RESERVED
     * → 앱 안내 문구 '이후 사용할 수 없는 닉네임입니다' (3.3.0)
     */
    public AvailabilityResponse checkNicknameAvailability(String nickname) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/auth/nickname-check");
    }

    /**
     * POST /api/auth/login — 로그인 (학생 앱·운영자 웹 공용) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 응답의 user.role로 운영자 여부를 구분한다.
     * - 탈퇴(WITHDRAWN) 회원은 로그인 불가
     * - 정지(SUSPENDED) 회원은 로그인 가능 (조회·채팅만 허용)
     * - fcmToken을 함께 보내면 USERS.fcm_token 갱신
     * - 발급 토큰 claim tv = 현재 USERS.token_version
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/auth/login");
    }

    /**
     * POST /api/auth/refresh — 액세스 토큰 재발급 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 리프레시 토큰은 DB에 저장하지 않는 서명 검증형 JWT (별도 테이블 없음).
     * 리프레시 토큰의 claim tv가 USERS.token_version과 다르거나 탈퇴 회원이면 401 TOKEN_REVOKED (3.3.0)
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/auth/refresh");
    }

    /**
     * POST /api/auth/logout — 로그아웃 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 해당 기기의 푸시 수신을 막기 위해 USERS.fcm_token을 NULL로 변경. 토큰 폐기는 앱에서 삭제로 처리
     */
    @Transactional
    public void logout(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/auth/logout");
    }
}
