package com.taxipot.domain.rating.service;

import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.rating.entity.Rating;
import com.taxipot.domain.rating.repository.RatingRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 담당: 회원·운영 — 스케줄러 작업 7 (인석 구현)
 * <p>처리 순서
 * <ol>
 *   <li>기본값을 넣어야 할 방 id 목록을 트랜잭션 없이 읽는다 (RatingRepository.findPotIdsMissingRatings)</li>
 *   <li>방마다 BOARDED 탑승자 목록과 이미 있는 평가쌍을 읽고, 빠진 쌍(평가자 ≠ 피평가자)만 골라낸다</li>
 *   <li>빠진 쌍을 한 건씩 RatingJobProcessor.insertDefault (건별 새 트랜잭션)로 저장한다</li>
 * </ol>
 * 회원이 같은 순간 평가를 제출해 UNIQUE 위반이 나면 그 쌍은 이미 채워진 것이므로 무시한다.
 * 다른 예외는 ERROR 로그를 남기고 다음 쌍으로 넘어가며, 빠진 쌍은 다음 실행(1시간 뒤) 때 다시 대상이 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingJobService {

    private final RatingRepository ratingRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final RatingJobProcessor ratingJobProcessor;
    private final TimeProvider timeProvider;
    private final AppProperties props;

    /**
     * 7. 평가 미제출 기본값 (1시간): 정산 완료 24시간 후에도 미제출인 평가쌍에 Rating.defaultOf (★3, is_default = 1, 평점 변동 없음).
     * 회원 제출과 겹쳐 UNIQUE 위반이 나도 배치가 멈추지 않게 건별로 처리하고 위반은 무시한다.
     */
    public void fillDefaultRatings() {
        // 정산 완료 + 24시간 <= now  ⇔  정산 완료 <= now − 24시간
        LocalDateTime threshold = timeProvider.now().minusHours(props.rating().periodHours());
        List<Long> potIds = ratingRepository.findPotIdsMissingRatings(threshold);

        int inserted = 0;
        for (Long potId : potIds) {
            try {
                inserted += fillPot(potId);
            } catch (Exception e) {
                log.error("스케줄러 7 평가 기본값 실패: potId={}", potId, e);
            }
        }
        if (inserted > 0) {
            log.info("스케줄러 7: 기본 평가 {}건 ({}개 방)", inserted, potIds.size());
        }
    }

    /** 방 하나의 빠진 평가쌍을 채우고, 새로 넣은 건수를 돌려준다 */
    private int fillPot(Long potId) {
        List<Long> boarded = taxipotMemberRepository.findMemberIds(potId, List.of(PotMemberStatus.BOARDED));

        // 이미 있는 평가쌍 "평가자:피평가자" (회원 제출·이전 실행분 모두 포함)
        Set<String> existing = new HashSet<>();
        for (Rating r : ratingRepository.findAllByPotId(potId)) {
            existing.add(r.getReviewerId() + ":" + r.getRevieweeId());
        }

        int inserted = 0;
        for (Long reviewer : boarded) {
            for (Long reviewee : boarded) {
                if (reviewer.equals(reviewee) || existing.contains(reviewer + ":" + reviewee)) {
                    continue;   // 본인 평가 없음 / 이미 있는 쌍
                }
                try {
                    ratingJobProcessor.insertDefault(potId, reviewer, reviewee);
                    inserted++;
                } catch (DataIntegrityViolationException e) {
                    // 목록을 읽은 뒤 회원이 먼저 제출함 → 이미 채워진 쌍이므로 무시 (명세)
                    log.debug("스케줄러 7: 이미 평가된 쌍 건너뜀 potId={}, {} → {}", potId, reviewer, reviewee);
                } catch (Exception e) {
                    log.error("스케줄러 7 기본 평가 저장 실패: potId={}, {} → {}", potId, reviewer, reviewee, e);
                }
            }
        }
        return inserted;
    }
}
