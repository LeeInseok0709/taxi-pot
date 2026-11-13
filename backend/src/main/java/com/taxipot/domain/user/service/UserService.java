package com.taxipot.domain.user.service;

import com.taxipot.domain.auth.catalog.BankCatalog;
import com.taxipot.domain.auth.dto.BankResponse;
import com.taxipot.domain.auth.dto.TokenResponse;
import com.taxipot.domain.auth.service.AuthService;
import com.taxipot.domain.user.dto.AccountResponse;
import com.taxipot.domain.user.dto.AccountUpdateRequest;
import com.taxipot.domain.user.dto.FcmTokenRequest;
import com.taxipot.domain.user.dto.MyProfileResponse;
import com.taxipot.domain.user.dto.PasswordChangeRequest;
import com.taxipot.domain.user.dto.UserPublicResponse;
import com.taxipot.domain.user.dto.UserUpdateRequest;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.mapper.UserMapper;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.util.Texts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    /** 계좌번호 자릿수 (가입 API 와 같은 기준: 숫자 10~14자리) */
    private static final int ACCOUNT_MIN_DIGITS = 10;
    private static final int ACCOUNT_MAX_DIGITS = 14;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    /** 비밀번호 변경 후 새 토큰 발급용 (CONVENTIONS 7장 공용 메서드 AuthService.tokens) */
    private final AuthService authService;

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
        User user = findUser(userId);

        // 1. 닉네임: 값이 왔고 지금과 다를 때만 검사·변경
        String nickname = request.nickname();
        if (nickname != null && !nickname.equals(user.getNickname())) {
            if (nickname.isBlank()) {
                throw Errors.invalidInput("nickname: 닉네임은 비워 둘 수 없습니다.");
            }
            if (Texts.isReservedNickname(nickname)) {
                throw new BusinessException(ErrorCode.RESERVED_NICKNAME);
            }
            // 사전 중복 검사 (동시 변경은 DB UNIQUE 위반 → DbConstraintTranslator 가 DUPLICATE_NICKNAME 으로 변환)
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.changeNickname(nickname);
        }

        // 2. 휴대폰번호: 형식(@Pattern)은 컨트롤러 @Valid 가 검사, 저장은 숫자만 (가입과 동일)
        if (request.phone() != null) {
            user.changePhone(Texts.digitsOnly(request.phone()));
        }

        // 3. 마케팅 수신 동의
        if (request.agreeMarketing() != null) {
            user.changeAgreeMarketing(request.agreeMarketing());
        }

        return UserMapper.toMyProfile(user);
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
        User user = findUser(userId);
        checkPassword(user, request.currentPassword());

        // pw_hash 교체 + token_version + 1 → 다른 기기의 기존 토큰은 401 TOKEN_REVOKED
        user.changePassword(passwordEncoder.encode(request.newPassword()));

        // 바뀐 token_version 으로 이 기기용 새 토큰 발급
        return authService.tokens(user);
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
        User user = findUser(userId);
        if (!user.hasAccount()) {
            throw Errors.notFound("출금 계좌");
        }
        return toAccountResponse(user);
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
        User user = findUser(userId);

        // 1. 현재 비밀번호 확인 (401 INVALID_CREDENTIALS)
        checkPassword(user, request.password());

        // 2. 은행 코드: GET /api/banks 목록에 있는 코드만
        if (!BankCatalog.exists(request.bankCode())) {
            throw Errors.invalidInput("bankCode: 지원하지 않는 은행 코드입니다.");
        }

        // 3. 계좌번호: 숫자만 남겨 10~14자리 (저장 시 EncryptedStringConverter 가 AES-256-GCM 암호화)
        String accountDigits = Texts.digitsOnly(request.accountNumber());
        if (accountDigits.length() < ACCOUNT_MIN_DIGITS || accountDigits.length() > ACCOUNT_MAX_DIGITS) {
            throw Errors.invalidInput("accountNumber: 계좌번호는 숫자 10~14자리여야 합니다.");
        }

        // 4. 예금주 = 가입자 이름 (본인 명의 계좌만)
        String holder = request.accountHolder().trim();
        if (user.getName() == null || !holder.equals(user.getName().trim())) {
            throw new BusinessException(ErrorCode.ACCOUNT_HOLDER_MISMATCH);
        }

        // 5. 은행·계좌번호·예금주를 한 번에 교체 (이미 신청된 출금은 신청 시점 스냅샷이라 영향 없음)
        user.registerAccount(request.bankCode(), accountDigits, holder);
        return toAccountResponse(user);
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
        User user = findUser(userId);
        // 실명·학번·연락처는 내보내지 않는다. 탈퇴 회원도 이용 기록 표시용으로 조회 가능 (isWithdrawn 으로 구분)
        return new UserPublicResponse(
                user.getId(),
                user.getNickname(),
                user.getUniversity(),
                user.getRatingAvg().doubleValue(),
                user.isWithdrawn());
    }

    // ---------- 내부 도우미 ----------

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원"));
    }

    /** 비밀번호 확인. 틀리면 401 INVALID_CREDENTIALS */
    private void checkPassword(User user, String rawPassword) {
        if (user.getPwHash() == null || !passwordEncoder.matches(rawPassword, user.getPwHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /** 계좌 응답: 은행 이름은 BankCatalog 에서, 계좌번호는 앞 3자리·뒤 4자리만 보이게 마스킹 */
    private AccountResponse toAccountResponse(User user) {
        String bankName = BankCatalog.BANKS.stream()
                .filter(b -> b.code().equals(user.getBankCode()))
                .map(BankResponse::name)
                .findFirst()
                .orElse(null);
        return new AccountResponse(
                user.getBankCode(),
                bankName,
                Texts.maskAccount(user.getAccountNo()),
                user.getAccountHolder());
    }
}
