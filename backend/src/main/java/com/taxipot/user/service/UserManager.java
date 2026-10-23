package com.taxipot.user.service;

import com.taxipot.global.code.UserStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.ErrorDetail;
import com.taxipot.global.util.TimeUtil;
import com.taxipot.user.entity.StudentVerification;
import com.taxipot.user.entity.User;
import com.taxipot.user.repository.StudentVerificationRepository;
import com.taxipot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * 회원 공용 기능 (Manager). 모든 도메인의 API 서비스·스케줄러가 주입해서 쓴다.
 * USERS 행을 바꾸는 공용 규칙(정지·노쇼)은 여기 한 곳에만 있다 — 다른 곳에서 USERS를 직접 수정하지 않는다.
 * Manager는 다른 도메인의 Manager·API 서비스를 주입하지 않는다 (최하위 계층).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManager {

    private static final DateTimeFormatter SUSPEND_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final AppProperties appProperties;

    /** 없으면 404 NOT_FOUND, 탈퇴 회원이면 403 WITHDRAWN_USER */
    public User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        return user;
    }

    /** 락 순서 1번 (USERS). 쓰기 트랜잭션 안에서만 호출 */
    @Transactional
    public User lockUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        return user;
    }

    /** 여러 회원을 id 오름차순으로 잠근다 (정산·방 취소·스케줄러) */
    @Transactional
    public List<User> lockUsers(Collection<Long> userIds) {
        return userRepository.findAllByIdInForUpdate(userIds);
    }

    /**
     * 방 생성·참여·입장 요청 공통 이용 조건 중 회원 부분 (명세 순서: 학생 인증 → 이용 정지).
     * 미수금은 UnpaidDebtInterceptor가 먼저 막고, 성별·진행 중인 방·인원·잔액은 pot 담당이 이어서 검사한다.
     */
    public void validateCanUsePot(User user) {
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            StudentVerification latest = studentVerificationRepository
                    .findFirstByMemberIdOrderByIdDesc(user.getId()).orElse(null);
            throw new BusinessException(ErrorCode.NOT_VERIFIED, ErrorDetail.of(
                    "emailStatus", latest == null ? null : latest.getEmailStatus(),
                    "reviewStatus", latest == null ? null : latest.getReviewStatus()));
        }
        if (isSuspendedNow(user)) {
            LocalDateTime until = user.getSuspendedUntil();
            String message = until == null
                    ? "영구 이용 정지된 계정입니다."
                    : "이용 정지 중입니다. %s 이후 다시 시도해 주세요.".formatted(until.format(SUSPEND_FORMAT));
            throw new BusinessException(ErrorCode.SUSPENDED_USER, message, ErrorDetail.of("suspendedUntil", until));
        }
    }

    /** 정지 만료 시각이 지났는데 스케줄러(5번)가 아직 안 돈 경우는 정지가 아닌 것으로 본다 */
    public boolean isSuspendedNow(User user) {
        if (user.getStatus() != UserStatus.SUSPENDED) {
            return false;
        }
        LocalDateTime until = user.getSuspendedUntil();
        return until == null || until.isAfter(TimeUtil.now());
    }

    /**
     * 모든 정지(노쇼 15분·신고 누적·운영자 수동)는 이 메서드로만 건다. 명세 3.3.0 '정지 규칙 통일':
     * 기존 정지와 새 정지 중 더 긴 쪽 유지. 영구(until = null) > 기간제, 기간제끼리는 더 늦은 시각. 이미 영구면 그대로.
     */
    @Transactional
    public void suspend(Long userId, LocalDateTime until) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            return;
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            LocalDateTime current = user.getSuspendedUntil();
            if (current == null) {
                return;
            }
            if (until != null && !until.isAfter(current)) {
                return;
            }
        }
        user.applySuspension(until);
    }

    /** 노쇼 처리 (스케줄러 1번, 집결 기한 이후 방장 내보내기): no_show_count + 1, 15분 정지(더 긴 쪽 유지) */
    @Transactional
    public void recordNoShow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.increaseNoShowCount();
        suspend(userId, TimeUtil.now().plusMinutes(appProperties.penalty().noShowSuspendMinutes()));
    }
}
