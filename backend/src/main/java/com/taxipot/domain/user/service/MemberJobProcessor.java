package com.taxipot.domain.user.service;

import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.domain.verification.repository.StudentVerificationRepository;
import com.taxipot.domain.wallet.entity.Withdrawal;
import com.taxipot.domain.wallet.repository.WithdrawalRepository;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 담당: 회원·운영(스케줄러 5·6 — 인석 구현)
 * <p>스케줄러 5·6 의 '회원 1명 처리'. 메서드 하나 = 트랜잭션 하나 = 회원 1명.
 * <p>MemberJobService 와 다른 빈으로 둔 이유: 같은 클래스 안에서 this. 로 부르면 @Transactional 이 적용되지 않는다 (CONVENTIONS 8장).
 * <p>락은 USERS 행 하나만 잡는다. 지갑·방을 건드리지 않으므로 락 순서(USERS → WALLET → TAXIPOT)의 첫 단계만 쓰는 셈이라 데드락이 생기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberJobProcessor {

    private final UserRepository userRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final FileStorage fileStorage;
    private final TimeProvider timeProvider;

    /**
     * 스케줄러 5: 회원 1명의 정지 만료 해제.
     * <p>목록을 읽은 뒤 잠그기 전 사이에 정지가 연장됐을 수 있으므로(노쇼 정지 등 User.suspend 는 더 긴 쪽을 남김),
     * 잠근 뒤 '아직 SUSPENDED 이고 해제 시각이 지났는가'를 다시 확인한다. 영구 정지(NULL)는 여기서 풀지 않는다.
     *
     * @return 실제로 해제했으면 true
     */
    @Transactional
    public boolean releaseSuspensionIfExpired(Long userId) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) {
            return false;
        }
        LocalDateTime now = timeProvider.now();
        if (user.getStatus() != UserStatus.SUSPENDED
                || user.getSuspendedUntil() == null          // 영구 정지: 운영자만 해제
                || user.getSuspendedUntil().isAfter(now)) {  // 그 사이 정지가 연장됨
            return false;
        }
        user.releaseSuspension();
        return true;
    }

    /**
     * 스케줄러 6: 탈퇴 회원 1명의 개인정보 파기.
     * <ol>
     *   <li>USERS 개인정보 파기 (User.purgePersonalInfo — 닉네임은 '탈퇴회원_{id}')</li>
     *   <li>WITHDRAWAL 행의 계좌 스냅샷 파기 (Withdrawal.purgeAccount — 출금 금액·상태 기록은 남김)</li>
     *   <li>STUDENT_VERIFICATION 행 삭제 + 학생증 캡처 파일 삭제</li>
     * </ol>
     * 파일은 커밋된 뒤에 지운다(deleteAfterCommit). 중간에 실패해 롤백되면 DB 는 그대로인데 파일만 사라지는 일을 막기 위해서다.
     *
     * @param threshold 이 시각 이전에 탈퇴한 회원만 파기 (now − 14일)
     * @return 실제로 파기했으면 true
     */
    @Transactional
    public boolean purgeIfDue(Long userId, LocalDateTime threshold) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null
                || !user.isWithdrawn()
                || user.getWithdrawnAt() == null
                || user.getWithdrawnAt().isAfter(threshold)
                || user.getEmail() == null) {               // 이미 파기됨
            return false;
        }

        user.purgePersonalInfo();

        List<Withdrawal> withdrawals = withdrawalRepository.findAllByMemberId(userId);
        withdrawals.forEach(Withdrawal::purgeAccount);

        List<StudentVerification> verifications = studentVerificationRepository.findAllByMemberId(userId);
        for (StudentVerification v : verifications) {
            fileStorage.deleteAfterCommit(v.getCaptureImgPath());   // null 이면 아무것도 안 함
        }
        studentVerificationRepository.deleteAll(verifications);

        log.info("탈퇴 회원 개인정보 파기: userId={}, 출금 계좌 {}건, 학생 인증 {}건",
                userId, withdrawals.size(), verifications.size());
        return true;
    }
}
