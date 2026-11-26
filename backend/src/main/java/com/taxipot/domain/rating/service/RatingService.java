package com.taxipot.domain.rating.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.rating.dto.RatingCreateRequest;
import com.taxipot.domain.rating.dto.RatingItem;
import com.taxipot.domain.rating.dto.RatingResponse;
import com.taxipot.domain.rating.dto.RatingTargetResponse;
import com.taxipot.domain.rating.entity.Rating;
import com.taxipot.domain.rating.repository.RatingRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 담당: 회원·운영 — 별점 (인석 구현)
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다. (여기서는 Repository 만 쓴다)
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    /** 피평가자 평점 변화 폭 (명세: ≥4 → +0.2, <3 → −0.2) */
    private static final BigDecimal RATING_STEP = new BigDecimal("0.2");
    private static final BigDecimal GOOD = new BigDecimal("4");
    private static final BigDecimal BAD = new BigDecimal("3");

    private final RatingRepository ratingRepository;
    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final AppProperties props;

    /**
     * <b>GET /api/pots/{potId}/ratings/targets</b> — 평가 대상 동승자 목록 조회
     * <p>
     * COMPLETED 방에서 나를 제외한 탑승자(BOARDED) 목록과 평가 제출 여부 (자동 기본 평가가 들어간 대상도 isRated = true)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     */
    @Transactional(readOnly = true)
    public List<RatingTargetResponse> getRatingTargets(Long userId, Long potId) {
        // 구현: 인석
        // 1. 나도 그 방 BOARDED 여야 한다 (끝까지 탑승한 사람끼리만 평가) → 403
        List<TaxipotMember> boarded = taxipotMemberRepository.findAllByPotIdAndStatus(potId, PotMemberStatus.BOARDED);
        if (boarded.stream().noneMatch(m -> m.getMemberId().equals(userId))) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }

        // 2. 나를 뺀 탑승자 + 닉네임 + 내가 이미 평가했는지 (기본 평가 포함)
        Set<Long> ratedByMe = ratingRepository.findAllByPotIdAndReviewerId(potId, userId).stream()
                .map(Rating::getRevieweeId).collect(Collectors.toSet());
        List<TaxipotMember> others = boarded.stream().filter(m -> !m.getMemberId().equals(userId)).toList();
        Map<Long, User> users = userRepository.findAllById(others.stream().map(TaxipotMember::getMemberId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return others.stream()
                .map(m -> new RatingTargetResponse(
                        m.getMemberId(),
                        users.containsKey(m.getMemberId()) ? users.get(m.getMemberId()).getNickname() : null,
                        m.getRole(),
                        ratedByMe.contains(m.getMemberId())))
                .toList();
    }

    /**
     * <b>POST /api/pots/{potId}/ratings</b> — 동승자 평가 제출 (여러 명 한 번에)
     * <p>
     * 평가 화면 한 번 제출로 동승자 여러 명을 함께 평가. 평가자·피평가자 모두 해당 방 BOARDED여야 하고 본인 평가 불가.
     * - UNIQUE(pot_id, reviewer_id, reviewee_id) — 대상자별 1회
     * - avg_score는 DB 생성 컬럼으로 자동 계산
     * - 피평가자 rating_avg: avg_score ≥ 4 → +0.2 / 3 ≤ avg_score &lt; 4 → 유지 / avg_score &lt; 3 → −0.2 (1.00~10.00 범위 고정)
     * - avg_score &lt; 3(별로였어요)이면 badReason 필수
     * - 요청 안의 항목은 하나의 트랜잭션 (하나라도 실패하면 전체 롤백)
     * - 평가 기간: 정산 완료(settled_at = 운영자 청구 승인 시각, 3.5.0) 후 24시간까지. 지나면 409 RATING_PERIOD_EXPIRED (그 뒤 스케줄러가 is_default = 1 기본 평가를 넣음, 3.3.0)
     * - 회원 제출 행은 is_default = 0. 스케줄러와 동시에 들어가 UNIQUE 위반이 나면 409 ALREADY_RATED로 변환
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT(별점 범위) / BAD_REASON_REQUIRED / SELF_RATING_NOT_ALLOWED
     * - 403: NOT_POT_MEMBER
     * - 409: INVALID_POT_STATUS(정산 미완료) / ALREADY_RATED / RATING_PERIOD_EXPIRED
     */
    @Transactional
    public List<RatingResponse> submitRatings(Long userId, Long potId, RatingCreateRequest request) {
        // 구현: 인석
        // 1. 평가자(나)가 그 방 BOARDED 인가 → 403
        Set<Long> boardedIds = taxipotMemberRepository.findAllByPotIdAndStatus(potId, PotMemberStatus.BOARDED)
                .stream().map(TaxipotMember::getMemberId).collect(Collectors.toSet());
        if (!boardedIds.contains(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }

        // 2. 정산이 끝난 방인가 → 409 INVALID_POT_STATUS
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        LocalDateTime settledAt = ratingRepository.findSettledAt(potId);
        if (pot.getStatus() != PotStatus.COMPLETED || settledAt == null) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        // 3. 평가 기간: 정산 완료 + 24시간 '전'까지만. 정각부터는 마감 → 409
        //    스케줄러 7은 '정산 완료 ≤ now − 24시간'인 방부터 ★3을 넣으므로, 두 경계가 정확히 이어진다 (틈·겹침 없음)
        LocalDateTime deadline = settledAt.plusHours(props.rating().periodHours());
        if (!timeProvider.now().isBefore(deadline)) {
            throw new BusinessException(ErrorCode.RATING_PERIOD_EXPIRED);
        }

        // 4. 항목별 검증 (하나라도 틀리면 아무것도 저장하지 않고 끝)
        Set<Long> seen = new HashSet<>();
        for (RatingItem item : request.ratings()) {
            Long revieweeId = item.revieweeId();
            if (revieweeId.equals(userId)) {
                throw new BusinessException(ErrorCode.SELF_RATING_NOT_ALLOWED);
            }
            if (!seen.add(revieweeId)) {
                throw Errors.invalidInput("같은 동승자를 한 요청에서 두 번 평가할 수 없습니다.");
            }
            if (!boardedIds.contains(revieweeId)) {
                throw new BusinessException(ErrorCode.NOT_POT_MEMBER);   // 피평가자도 BOARDED 여야 함
            }
            // 별점 1~5 범위는 DTO 의 @Min/@Max 가 400 INVALID_INPUT 으로 막는다
            if (average(item).compareTo(BAD) < 0 && (item.badReason() == null || item.badReason().isBlank())) {
                throw new BusinessException(ErrorCode.BAD_REASON_REQUIRED);
            }
            if (ratingRepository.existsByPotIdAndReviewerIdAndRevieweeId(potId, userId, revieweeId)) {
                throw new BusinessException(ErrorCode.ALREADY_RATED);
            }
        }

        // 5. 피평가자 USERS 를 id 오름차순으로 잠근다 (평점을 바꾸므로. 다른 사람이 동시에 같은 사람을 평가해도 덮어쓰지 않게)
        Map<Long, User> reviewees = userRepository.findAllByIdInForUpdate(seen).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 6. 저장 + 평점 반영. saveAndFlush 로 바로 INSERT 해서, 스케줄러 7과 겹친 UNIQUE 위반을 여기서 409 로 바꾼다
        List<RatingResponse> result = new ArrayList<>();
        for (RatingItem item : request.ratings()) {
            Rating saved;
            try {
                saved = ratingRepository.saveAndFlush(Rating.submit(potId, userId, item.revieweeId(),
                        item.noshowScore(), item.mannerScore(), item.communicationScore(), item.paymentScore(),
                        blankToNull(item.badReason())));
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException(ErrorCode.ALREADY_RATED);   // 트랜잭션 전체 롤백 (명세: 하나라도 실패하면 전체)
            }
            User reviewee = reviewees.get(item.revieweeId());
            BigDecimal delta = ratingDelta(saved.averageScore());
            if (reviewee != null && delta.signum() != 0) {
                reviewee.changeRating(delta);   // 1.00~10.00 범위 고정은 User.changeRating 이 처리
            }
            result.add(toResponse(saved));
        }
        return result;
    }

    /** 4항목 평균 (Rating.averageScore 와 같은 계산, 저장 전 검증용) */
    private static BigDecimal average(RatingItem item) {
        return BigDecimal.valueOf(item.noshowScore() + item.mannerScore() + item.communicationScore() + item.paymentScore())
                .divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
    }

    /** 평균 ≥ 4 → +0.2, 3 ≤ 평균 < 4 → 0, 평균 < 3 → −0.2 */
    private static BigDecimal ratingDelta(BigDecimal avg) {
        if (avg.compareTo(GOOD) >= 0) {
            return RATING_STEP;
        }
        if (avg.compareTo(BAD) < 0) {
            return RATING_STEP.negate();
        }
        return BigDecimal.ZERO;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    /** avg_score 는 DB 생성 컬럼이라 저장 직후 엔티티에는 없으므로 같은 식으로 계산한 값을 내려준다 */
    private static RatingResponse toResponse(Rating r) {
        return new RatingResponse(r.getId(), r.getPotId(), r.getReviewerId(), r.getRevieweeId(),
                r.getNoshowScore(), r.getMannerScore(), r.getCommunicationScore(), r.getPaymentScore(),
                r.averageScore().doubleValue(), r.getBadReason(), r.getIsDefault(), Kst.toOffset(r.getCreatedAt()));
    }
}
