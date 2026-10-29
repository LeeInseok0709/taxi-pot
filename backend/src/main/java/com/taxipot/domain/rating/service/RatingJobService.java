package com.taxipot.domain.rating.service;

import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 담당: 회원·운영 — 스케줄러 작업 7 */
@Service
@RequiredArgsConstructor
public class RatingJobService {

    /**
     * 7. 평가 미제출 기본값 (1시간): 정산 완료 24시간 후에도 미제출인 평가쌍에 Rating.defaultOf (★3, is_default = 1, 평점 변동 없음).
     * 회원 제출과 겹쳐 UNIQUE 위반이 나도 배치가 멈추지 않게 건별로 처리하고 위반은 무시한다.
     */
    public void fillDefaultRatings() {
        throw new NotImplementedYetException("스케줄러 7 평가 미제출 기본값");
    }
}
