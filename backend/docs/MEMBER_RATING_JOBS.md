# 스케줄러 5·6·7 구현 정리

`is/member-rating-jobs` 브랜치(기준: `develop`, PR #13 머지 이후)에서 구현한 내용입니다. 담당 표(`CONVENTIONS.md` 1장)로는 회원·운영 칸이지만 맡은 PR이 없어 인석이 가져왔습니다. 채팅(택시팟 칸)은 서연님 `sy/chat`에 구현되어 있어 여기서는 다루지 않습니다.

## 1. 바뀐 파일

경로는 모두 `src/main/java/com/taxipot/domain/` 아래입니다.

| 파일 | 구분 | 내용 |
|---|---|---|
| `user/service/MemberJobService.java` | 수정 | 스케줄러 5·6 — 대상 회원 id를 읽고 회원마다 Processor 호출 |
| `user/service/MemberJobProcessor.java` | 새 파일 | 스케줄러 5·6 — '회원 1명 처리'. 메서드 하나 = 트랜잭션 하나 |
| `user/repository/UserRepository.java` | 수정 | 대상 회원 id 조회 쿼리 2개 |
| `rating/service/RatingJobService.java` | 수정 | 스케줄러 7 — 대상 방을 읽고 빠진 평가쌍을 골라냄 |
| `rating/service/RatingJobProcessor.java` | 새 파일 | 스케줄러 7 — 평가쌍 1개 저장 (건별 새 트랜잭션) |
| `rating/repository/RatingRepository.java` | 수정 | 대상 방 id 조회 쿼리, 방의 평가 행 전체 조회 |

`User`, `Withdrawal`, `StudentVerification`, `Rating` 엔티티와 `global/`은 수정하지 않았습니다. 엔티티에 미리 만들어 둔 메서드(`releaseSuspension`, `purgePersonalInfo`, `purgeAccount`, `Rating.defaultOf`)와 기존 조회 메서드(`findAllByMemberId`, `findMemberIds`)만 썼습니다. 다른 사람 브랜치와 겹치는 파일도 없습니다.

## 2. 스케줄러

주기와 on/off는 기존 `TaxipotScheduler`와 `app.scheduler.*` 설정을 그대로 씁니다.

| 번호 | 주기 | 대상 | 처리 |
|---|---|---|---|
| 5 정지 만료 해제 | 1분 | SUSPENDED이고 `suspended_until ≤ now` (NULL = 영구 정지는 제외) | `User.releaseSuspension()` → ACTIVE |
| 6 탈퇴 회원 파기 | 매일 04:00 | WITHDRAWN이고 `withdrawn_at ≤ now − 14일`, 아직 파기 전 | USERS 개인정보 파기, WITHDRAWAL 계좌 스냅샷 파기, STUDENT_VERIFICATION 행 삭제 + 캡처 파일 삭제 |
| 7 평가 기본값 | 1시간 | COMPLETED 방, 정산 완료 + 24시간 경과, BOARDED끼리의 평가쌍 중 RATING이 없는 쌍 | 빠진 쌍마다 ★3 기본 평가(`is_default = 1`), 평점 변동 없음 |

기준 숫자 14일·24시간은 `app.member.withdrawn-purge-days`, `app.rating.period-hours`에서 읽습니다. 쿼리에는 `withdrawn_at + 14일 ≤ now` 대신 `withdrawn_at ≤ now − 14일`처럼 넘겨, 컬럼에 계산을 걸지 않고 인덱스를 탈 수 있게 했습니다.

## 3. 처리 방식

모양은 스케줄러 1~4(`PotJobService`)와 같습니다. "번호표 뽑기 → 창구에서 서류 다시 확인 → 처리" 순서입니다.

1. 대상 id 목록을 트랜잭션·락 없이 읽습니다.
2. 하나씩 Processor의 `@Transactional` 메서드를 부릅니다. 같은 클래스에서 `this.`로 부르면 트랜잭션이 걸리지 않아서 빈을 나눴습니다(`CONVENTIONS.md` 8장).
3. 한 건에서 예외가 나면 그 건만 롤백하고 ERROR 로그(스택 포함)를 남긴 뒤 다음 건을 처리합니다. 실패한 건은 다음 실행 때 다시 대상이 됩니다.

### 스케줄러 5·6: 잠근 뒤 다시 확인

USERS 행을 `findByIdForUpdate`로 잠근 뒤 조건을 다시 봅니다. 목록을 읽은 사이 노쇼 정지가 연장됐으면(`User.suspend`는 더 긴 쪽을 남김) 해제하지 않습니다. 지갑·방은 건드리지 않으므로 USERS 하나만 잠그고, 락 순서(USERS → WALLET → TAXIPOT)의 첫 단계만 쓰는 셈이라 데드락이 생기지 않습니다.

스케줄러 6의 '아직 파기 전'은 email이 남아 있는지로 판단합니다. 탈퇴 전에는 DB CHECK(`chk_users_pii_required`)로 email이 반드시 있고, 파기하면 NULL이 되기 때문입니다. 학생증 캡처 파일은 `deleteAfterCommit`으로 커밋된 뒤에 지웁니다. 중간에 실패해 DB가 롤백됐는데 파일만 사라지는 일을 막기 위해서입니다.

### 스케줄러 7: 건별 트랜잭션

명세의 "회원 제출과 겹쳐 UNIQUE 위반이 나도 배치가 멈추지 않게 건별로 처리하고 위반은 무시한다"를 그대로 따랐습니다. 평가쌍 하나마다 `REQUIRES_NEW` 트랜잭션으로 `saveAndFlush`하고, `UNIQUE(pot_id, reviewer_id, reviewee_id)` 위반(`DataIntegrityViolationException`)이 나면 그 쌍은 회원이 먼저 제출한 것이므로 건너뜁니다. 모든 쌍이 채워진 방은 쿼리에서 자동으로 빠지므로 같은 방을 매시간 다시 처리하지 않습니다.

## 4. 팀 확인 필요

1. **정산 완료 시각**: 방 단위 컬럼이 없어 그 방 SETTLEMENT 행들의 `settled_at` 최댓값(= 운영자 청구 승인 시각)을 기준으로 했습니다. 평가 제출 API(`RatingService.submitRatings`)의 '평가 기간 24시간' 판단도 같은 기준이어야 합니다. 그래야 API는 마감인데 스케줄러는 아직 안 채우는(또는 그 반대) 틈이 생기지 않습니다.
2. **출금 처리 중인 탈퇴 회원**: 14일이 지나도 REQUESTED 상태 출금이 남아 있으면 계좌 스냅샷이 지워져 송금할 수 없게 됩니다. 탈퇴 시 진행 중인 출금을 막는지 지갑 담당과 확인이 필요합니다.
3. **알림**: 정지 해제·기본 평가 때 알림을 보내라는 명세가 없어 보내지 않습니다.

## 5. 확인 방법

`backend`에서 `.\gradlew.bat compileJava`로 컴파일을 확인합니다. 새 JPQL 쿼리 3개는 서버를 띄울 때 검사되므로 서버를 한 번 실행해 에러 없이 뜨는지 봅니다. 스케줄러는 demo 프로필(`app.scheduler.enabled: true`)에서만 돕니다.
