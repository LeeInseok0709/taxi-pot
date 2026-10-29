package com.taxipot.domain.rating.service;

import com.taxipot.domain.rating.dto.RatingCreateRequest;
import com.taxipot.domain.rating.dto.RatingResponse;
import com.taxipot.domain.rating.dto.RatingTargetResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

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
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/pots/{potId}/ratings/targets");
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
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/ratings");
    }
}
