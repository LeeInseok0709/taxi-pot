package com.taxipot.rating.service;

import com.taxipot.rating.dto.RatingCreateRequest;
import com.taxipot.rating.dto.RatingResponse;
import com.taxipot.rating.dto.RatingTargetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * RatingController 전용 API 서비스. 담당: 효우
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    /**
     * GET /api/pots/{potId}/ratings/targets — 평가 대상 동승자 목록 조회 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * COMPLETED 방에서 나를 제외한 탑승자(BOARDED) 목록과 평가 제출 여부 (자동 기본 평가가 들어간 대상도 isRated = true)
     */
    public List<RatingTargetResponse> getRatingTargetList(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/pots/{potId}/ratings/targets");
    }

    /**
     * POST /api/pots/{potId}/ratings — 동승자 평가 제출 (여러 명 한 번에) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 평가 화면 한 번 제출로 동승자 여러 명을 함께 평가. 평가자·피평가자 모두 해당 방 BOARDED여야 하고 본인 평가 불가.
     * - UNIQUE(pot_id, reviewer_id, reviewee_id) — 대상자별 1회
     * - avg_score는 DB 생성 컬럼으로 자동 계산
     * - 피평가자 rating_avg: avg_score ≥ 4 → +0.2 / 3 ≤ avg_score < 4 → 유지 / avg_score < 3 → −0.2 (1.00~10.00 범위 고정)
     * - avg_score < 3(별로였어요)이면 badReason 필수
     * - 요청 안의 항목은 하나의 트랜잭션 (하나라도 실패하면 전체 롤백)
     * - 평가 기간: 정산 완료(settled_at = 운영자 청구 승인 시각, 3.5.0) 후 24시간까지. 지나면 409 RATING_PERIOD_EXPIRED (그 뒤 스케줄러가 is_default = 1 기본 평가를 넣음, 3.3.0)
     * - 회원 제출 행은 is_default = 0. 스케줄러와 동시에 들어가 UNIQUE 위반이 나면 409 ALREADY_RATED로 변환
     */
    @Transactional
    public List<RatingResponse> createRatings(Long userId, Long potId, RatingCreateRequest request) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/pots/{potId}/ratings");
    }
}
