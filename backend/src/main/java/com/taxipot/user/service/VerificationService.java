package com.taxipot.user.service;

import com.taxipot.user.dto.EmailCodeConfirmRequest;
import com.taxipot.user.dto.EmailCodeSendResponse;
import com.taxipot.user.dto.VerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * VerificationController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationService {

    /**
     * POST /api/verifications/email-code — 이메일 인증코드 재발송 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * email_status = PENDING인 가장 최근 STUDENT_VERIFICATION 행의 email_code와 email_code_expires_at(발송 시각 + 1분, 서버 설정값 — 3.9.0에서 5분 → 1분)을 갱신하고
     * email_code_attempts = 0으로 초기화한 뒤 재발송한다 (3.3.0).
     * (최초 발송은 회원가입 시 자동 처리)
     * - 회원의 인증 행 중 email_status = VERIFIED인 행이 하나라도 있으면 이메일 인증 완료로 보고 409
     * - 재발송 간격 (3.9.0): 직전 발송 후 60초(서버 설정값 `app.verification.resend-interval-seconds=60`)가 지나야 재발송 가능
     *   - 직전 발송 시각 = email_code_expires_at − 1분 (가입 시 최초 발송도 포함)
     *   - 60초 안의 요청은 코드·만료 시각·실패 횟수를 바꾸지 않고 메일도 보내지 않은 채 429 TOO_MANY_REQUESTS (detail = null)
     *   - 재발송 때 실패 횟수가 0으로 초기화되므로, 이 간격이 인증코드 반복 대입을 막는 역할도 한다
     */
    @Transactional
    public EmailCodeSendResponse sendEmailCode(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/verifications/email-code");
    }

    /**
     * POST /api/verifications/email-code/confirm — 이메일 인증코드 확인 (1차 인증) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * email_status = PENDING인 가장 최근 인증 행의 코드·만료 시각을 검증하고 email_status = VERIFIED로 변경.
     * 캡처본 재제출로 생긴 행(email_status = VERIFIED로 복사됨)의 email_code는 검증 대상이 아니다.
     * - 입력 실패 제한 (3.3.0): 검증 순서 = 실패 횟수(email_code_attempts ≥ 5면 409 EMAIL_CODE_ATTEMPTS_EXCEEDED) → 만료(EXPIRED_EMAIL_CODE) → 코드 일치
     *   - 코드가 틀리면 email_code_attempts + 1 후 400 INVALID_EMAIL_CODE (detail.remainingAttempts = 5 − 증가 후 값)
     *   - 5회째 실패하면 remainingAttempts = 0, 이후에는 맞는 코드여도 EMAIL_CODE_ATTEMPTS_EXCEEDED → 재발송으로만 해제
     *   - 주의(Spring): 실패 횟수 증가는 예외를 던져도 커밋돼야 하므로 `@Transactional(noRollbackFor = BusinessException.class)` 또는
     *     별도 트랜잭션(`REQUIRES_NEW`)에서 증가시킨다. 그냥 예외를 던지면 롤백되어 횟수가 늘지 않음
     *   - 동시 입력으로 6 이상이 되지 않게 `UPDATE ... SET email_code_attempts = email_code_attempts + 1 WHERE id = ? AND email_code_attempts < 5`로 증가
     */
    @Transactional
    public VerificationResponse confirmEmailCode(Long userId, EmailCodeConfirmRequest request) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/verifications/email-code/confirm");
    }

    /**
     * POST /api/verifications/capture — 학생 기본 정보 캡처본 제출 (2차 인증, 반려 후 재제출 포함) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 종합정보시스템 학생 기본 정보 화면(학번·학적·성별) 캡처를 업로드한다.
     * - 이메일 인증(VERIFIED) 완료 후에만 제출 가능
     * - 제출 학번이 가입 학번(USERS.student_no)과 다르면 즉시 400 (STUDENT_NO_MISMATCH)
     * - 이미지는 서버 로컬 폴더(`{app.upload-dir}/captures/{memberId}/{UUID}.jpg|png`)에 저장하고 capture_img_path에 상대 경로 저장 (3.3.0)
     * - 최근 행이 캡처 미제출(review_status = NOT_SUBMITTED)이면 그 행에 capture_img_path 저장 + review_status = PENDING
     * - 최근 행이 PENDING이면 409 REVIEW_IN_PROGRESS, APPROVED면 409 ALREADY_VERIFIED
     * - 반려(REJECTED) 후 재제출이면 새 행 생성. 이메일 인증 정보와 캡처 심사 정보는 아래처럼 분리해서 다룬다
     *   - email_status: 직전 행의 VERIFIED를 그대로 복사 (이메일 인증은 회원 단위로 한 번만 하면 됨)
     *   - email_code·email_code_expires_at: NOT NULL 컬럼이라 직전 행 값을 복사하지만 '보관용 값'일 뿐 검증에 쓰지 않음 (만료 여부와 무관)
     *   - review_status = PENDING, reject_reason·reviewed_at = NULL (심사는 새 행 기준)
     * - 이메일 인증 여부 판단: 회원의 행 중 email_status = VERIFIED가 하나라도 있으면 완료 / 캡처 심사 상태: 가장 최근 행의 review_status
     * - review_status = PENDING으로 운영자 심사 대기 (DB CHECK: NOT_SUBMITTED ⇔ capture_img_path NULL)
     */
    @Transactional
    public VerificationResponse submitCapture(Long userId, MultipartFile captureImage, String studentNo) {
        throw new UnsupportedOperationException("TODO(기성): POST /api/verifications/capture");
    }

    /**
     * GET /api/verifications/me — 내 인증 진행 상태 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 가장 최근 STUDENT_VERIFICATION 행 기준.
     * 앱 화면 분기: emailStatus = PENDING → 이메일 인증 / reviewStatus = NOT_SUBMITTED → 캡처 제출 / PENDING → 심사 대기 / REJECTED → 반려 사유 + 재제출 / APPROVED → 완료
     */
    public VerificationResponse getMyVerification(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/verifications/me");
    }
}
