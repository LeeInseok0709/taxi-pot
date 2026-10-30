package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.domain.verification.repository.StudentVerificationRepository;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.web.UnpaidDebtQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 명세 '이용 조건 검증 순서' (방 생성·참여·입장 요청 공통):
 * 학생 인증 → 이용 정지 → 미수금 → 성별 제한 → 진행 중인 다른 방 → 모집 인원 → 지갑 잔액.
 * <p>순서가 정해져 있으므로 세 API 모두 이 클래스를 쓴다. USERS·WALLET·TAXIPOT 락을 잡은 뒤 호출한다.
 */
@Component
@RequiredArgsConstructor
public class PotEligibilityValidator {

    private final StudentVerificationRepository verificationRepository;
    private final TaxipotMemberRepository memberRepository;
    private final UnpaidDebtQuery unpaidDebtQuery;
    private final TimeProvider timeProvider;

    /**
     * @param pot 참여·입장 요청 대상 방 (방 생성이면 null → 성별은 requestedGender 로, 인원 검사 생략)
     * @param requestedGender 방 생성 시 요청한 성별 제한 (참여면 null)
     */
    public void validate(User user, Wallet wallet, Taxipot pot, GenderOption requestedGender, int holdAmount) {
        // 1. 학생 인증
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            StudentVerification latest = verificationRepository.findFirstByMemberIdOrderByIdDesc(user.getId()).orElse(null);
            throw Errors.notVerified(latest == null ? null : latest.getEmailStatus(),
                    latest == null ? null : latest.getReviewStatus());
        }
        // 2. 이용 정지
        if (user.isSuspendedAt(timeProvider.now())) {
            throw Errors.suspended(user.getSuspendedUntil());
        }
        // 3. 미수금 (보통은 DebtRestrictionInterceptor 가 먼저 막는다)
        unpaidDebtQuery.findUnpaid(user.getId()).ifPresent(d -> {
            throw Errors.unpaidDebtExists(d.totalAmount(), d.earliestDueAt());
        });
        // 4. 성별 제한
        GenderOption option = pot != null ? pot.getGenderOption() : requestedGender;
        if (!genderAllowed(user.getGender(), option)) {
            throw Errors.genderRestricted(option);
        }
        // 5. 진행 중인 다른 방
        List<Long> active = memberRepository.findActivePotIds(user.getId());
        for (Long potId : active) {
            if (pot == null || !potId.equals(pot.getId())) {
                throw Errors.activePotExists(potId);
            }
        }
        // 6. 모집 인원
        if (pot != null && pot.isFull()) {
            throw Errors.potFull(pot.getMaxMembers(), pot.getCurrentMembers());
        }
        // 7. 지갑 잔액
        if (wallet.getBalance() < holdAmount) {
            throw Errors.insufficientBalance(holdAmount, wallet.getBalance());
        }
    }

    /** 성별 매핑: USERS.gender M ↔ MALE, F ↔ FEMALE */
    public static boolean genderAllowed(Gender gender, GenderOption option) {
        if (option == null || option == GenderOption.ALL) {
            return true;
        }
        if (gender == null) {
            throw new BusinessException(ErrorCode.GENDER_RESTRICTED);
        }
        return (option == GenderOption.MALE && gender == Gender.M) || (option == GenderOption.FEMALE && gender == Gender.F);
    }
}
