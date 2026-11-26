package com.taxipot.domain.user.service;

import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * 담당: 회원·운영 — 스케줄러 작업 5·6 (인석 구현)
 * <p>모양은 PotJobService 와 같다.
 * <ol>
 *   <li>대상 회원 id 목록을 트랜잭션·락 없이 읽는다</li>
 *   <li>회원마다 MemberJobProcessor 의 @Transactional 메서드를 부른다 (그 안에서 USERS 를 잠그고 조건을 다시 확인)</li>
 *   <li>한 회원에서 예외가 나도 그 회원만 롤백되고 나머지는 계속 처리한다. 실패한 회원은 다음 실행 때 다시 대상이 된다</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberJobService {

    private final UserRepository userRepository;
    private final MemberJobProcessor memberJobProcessor;
    private final TimeProvider timeProvider;
    private final AppProperties props;

    /** 5. 정지 만료 해제 (1분): suspended_until &lt;= now 인 SUSPENDED → ACTIVE (User.releaseSuspension). NULL(영구)은 제외 */
    public void releaseExpiredSuspensions() {
        List<Long> ids = userRepository.findIdsToReleaseSuspension(timeProvider.now());
        int done = forEachMember("5 정지 만료 해제", ids, memberJobProcessor::releaseSuspensionIfExpired);
        if (done > 0) {
            log.info("스케줄러 5: 정지 해제 {}명", done);
        }
    }

    /**
     * 6. 탈퇴 회원 개인정보 파기 (일 1회): withdrawn_at + 14일 &lt;= now → User.purgePersonalInfo(),
     * WITHDRAWAL 계좌 스냅샷 파기(Withdrawal.purgeAccount), STUDENT_VERIFICATION 행 DELETE + 캡처 파일 삭제(FileStorage.delete)
     */
    public void purgeWithdrawnMembers() {
        // withdrawn_at + 14일 <= now  ⇔  withdrawn_at <= now − 14일 (컬럼에 계산을 걸지 않아야 인덱스를 탈 수 있다)
        LocalDateTime threshold = timeProvider.now().minusDays(props.member().withdrawnPurgeDays());
        List<Long> ids = userRepository.findIdsToPurge(threshold);
        int done = forEachMember("6 탈퇴 회원 파기", ids, id -> memberJobProcessor.purgeIfDue(id, threshold));
        if (done > 0) {
            log.info("스케줄러 6: 개인정보 파기 {}명", done);
        }
    }

    /**
     * 회원마다 처리하고, 실제로 처리한(true) 수를 돌려준다.
     * 예외는 삼키지 않고 ERROR 로그(스택 포함)를 남긴 뒤 다음 회원으로 넘어간다 (CONVENTIONS 9장).
     */
    private int forEachMember(String job, List<Long> ids, Predicate<Long> task) {
        int done = 0;
        for (Long id : ids) {
            try {
                if (task.test(id)) {
                    done++;
                }
            } catch (Exception e) {
                log.error("스케줄러 {} 실패: userId={}", job, id, e);
            }
        }
        return done;
    }
}
