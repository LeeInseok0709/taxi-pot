package com.taxipot.domain.rating.service;

import com.taxipot.domain.rating.entity.Rating;
import com.taxipot.domain.rating.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영(스케줄러 7 — 인석 구현)
 * <p>스케줄러 7 의 '평가쌍 1개 저장'. 명세: "회원 제출과 겹쳐 UNIQUE 위반이 나도 배치가 멈추지 않게 건별로 처리하고 위반은 무시한다."
 * <p>그래서 한 쌍마다 새 트랜잭션(REQUIRES_NEW)으로 저장한다. UNIQUE(pot_id, reviewer_id, reviewee_id) 위반이 나면
 * 이 쌍의 트랜잭션만 롤백되고, 호출한 RatingJobService 가 DataIntegrityViolationException 을 받아 무시한다.
 */
@Component
@RequiredArgsConstructor
public class RatingJobProcessor {

    private final RatingRepository ratingRepository;

    /**
     * ★3 기본 평가 1건 저장 (is_default = 1). 평점(rating_avg)은 바꾸지 않는다 (명세: 평점 변동 없음).
     * <p>saveAndFlush 로 INSERT 를 바로 실행해, UNIQUE 위반이 이 메서드 안에서 터지게 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertDefault(Long potId, Long reviewerId, Long revieweeId) {
        ratingRepository.saveAndFlush(Rating.defaultOf(potId, reviewerId, revieweeId));
    }
}
