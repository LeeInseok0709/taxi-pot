package com.taxipot.domain.auth.service;

import com.taxipot.domain.auth.catalog.BankCatalog;
import com.taxipot.domain.auth.dto.AvailabilityResponse;
import com.taxipot.domain.auth.dto.BankResponse;
import com.taxipot.domain.auth.dto.LoginRequest;
import com.taxipot.domain.auth.dto.LoginResponse;
import com.taxipot.domain.auth.dto.RefreshRequest;
import com.taxipot.domain.auth.dto.SignupRequest;
import com.taxipot.domain.auth.dto.TokenResponse;
import com.taxipot.domain.auth.dto.UniversityResponse;
import com.taxipot.domain.university.entity.University;
import com.taxipot.domain.university.repository.UniversityRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.mapper.UserMapper;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.verification.service.VerificationService;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.security.JwtProvider;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.util.Texts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 담당: 공통·인증 (구현 완료) — 가입·로그인·토큰 재발급·로그아웃·닉네임 확인·학교/은행 목록 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final WalletLedger walletLedger;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TimeProvider timeProvider;

    /**
     * POST /api/auth/signup — USERS(is_verified = 0) + WALLET(0원) + STUDENT_VERIFICATION(PENDING) 생성, 인증코드 발송, 토큰 발급.
     * 검증 순서: 입력 형식 → 닉네임 예약어 → 학교 → 이메일 도메인 → 예금주 → 중복(이메일·닉네임·학번)
     */
    @Transactional
    public LoginResponse signup(SignupRequest r) {
        if (!Boolean.TRUE.equals(r.agreeRequired())) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }
        String email = Texts.normalizeEmail(r.email());
        String phone = Texts.digitsOnly(r.phone());
        String account = Texts.digitsOnly(r.accountNumber());
        if (!BankCatalog.exists(r.bankCode())) {
            throw Errors.invalidInput("bankCode: 지원하지 않는 은행 코드입니다.");
        }
        if (account.length() < 10 || account.length() > 14) {
            throw Errors.invalidInput("accountNumber: 계좌번호는 숫자 10~14자리여야 합니다.");
        }
        if (Texts.isReservedNickname(r.nickname())) {
            throw new BusinessException(ErrorCode.RESERVED_NICKNAME);
        }
        University university = universityRepository.findByNameAndIsActiveTrue(r.university())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_UNIVERSITY));
        if (!Texts.emailDomain(email).equals(university.getEmailDomain())) {
            throw new BusinessException(ErrorCode.EMAIL_DOMAIN_MISMATCH);
        }
        if (!r.accountHolder().trim().equals(r.name().trim())) {
            throw new BusinessException(ErrorCode.ACCOUNT_HOLDER_MISMATCH);
        }
        // 사전 중복 검사 (동시 가입은 DB UNIQUE 위반 → DbConstraintTranslator 가 같은 코드로 변환)
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(r.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (userRepository.existsByUniversityAndStudentNo(university.getName(), r.studentNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT_NO);
        }

        User user = userRepository.saveAndFlush(User.signUp(email, passwordEncoder.encode(r.password()), r.name().trim(),
                r.nickname(), phone, r.gender(), university.getName(), r.studentNo(),
                r.bankCode(), account, r.accountHolder().trim(), Boolean.TRUE.equals(r.agreeMarketing()),
                timeProvider.now()));
        walletLedger.open(user.getId());
        verificationService.issueForSignup(user);
        return loginResponse(user);
    }

    /** GET /api/auth/nickname-check */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkNickname(String nickname) {
        if (Texts.isReservedNickname(nickname)) {
            return new AvailabilityResponse(false, AvailabilityResponse.Reason.RESERVED);
        }
        if (userRepository.existsByNickname(nickname)) {
            return new AvailabilityResponse(false, AvailabilityResponse.Reason.DUPLICATE);
        }
        return new AvailabilityResponse(true, null);
    }

    /** GET /api/universities */
    @Transactional(readOnly = true)
    public List<UniversityResponse> getUniversities() {
        return universityRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(u -> new UniversityResponse(u.getId(), u.getName(), u.getEmailDomain()))
                .toList();
    }

    /** GET /api/banks */
    public List<BankResponse> getBanks() {
        return BankCatalog.BANKS;
    }

    /** POST /api/auth/login — 정지 회원은 로그인 가능, 탈퇴 회원은 403 WITHDRAWN_USER */
    @Transactional
    public LoginResponse login(LoginRequest r) {
        User user = userRepository.findByEmail(Texts.normalizeEmail(r.email()))
                .filter(u -> u.getPwHash() != null && passwordEncoder.matches(r.password(), u.getPwHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        if (r.fcmToken() != null && !r.fcmToken().isBlank()) {
            user.changeFcmToken(r.fcmToken());
        }
        return loginResponse(user);
    }

    /** POST /api/auth/refresh — tv 불일치·탈퇴면 401 TOKEN_REVOKED. 리프레시 토큰도 새로 발급 */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest r) {
        JwtProvider.TokenClaims claims = jwtProvider.parse(r.refreshToken(), JwtProvider.TYPE_REFRESH);
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_REVOKED));
        if (user.isWithdrawn() || user.getTokenVersion() != claims.tokenVersion()) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }
        return tokens(user);
    }

    /** POST /api/auth/logout — 이 기기 푸시 중단 (fcm_token = NULL). 토큰 폐기는 앱에서 삭제 */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(u -> u.changeFcmToken(null));
    }

    /** 비밀번호 변경 등 token_version 이 바뀐 뒤 새 토큰이 필요할 때 다른 서비스에서도 사용 */
    public TokenResponse tokens(User user) {
        return new TokenResponse(
                jwtProvider.createAccessToken(user.getId(), user.getRole(), user.getTokenVersion()),
                jwtProvider.createRefreshToken(user.getId(), user.getRole(), user.getTokenVersion()),
                "Bearer", jwtProvider.accessTokenExpiresInSeconds());
    }

    private LoginResponse loginResponse(User user) {
        TokenResponse t = tokens(user);
        return new LoginResponse(t.accessToken(), t.refreshToken(), t.tokenType(), t.expiresIn(), UserMapper.toMyProfile(user));
    }
}
