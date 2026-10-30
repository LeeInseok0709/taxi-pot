# 도착 확인·내보내기·스케줄러 구현 정리

`feat/arrival-scheduler` 브랜치(기준: `sy/develop`)에서 구현한 내용입니다. 담당은 택시팟(인석)이며, 도착 확인·내보내기 API 2개와 스케줄러 1·2·3·4·11을 구현했습니다. 기준 문서는 API 명세서 3.9.0과 `CONVENTIONS.md`이고, 명세에 없어 임의로 정한 부분은 6장에 모았습니다.

## 1. 바뀐 파일

경로는 모두 `src/main/java/com/taxipot/domain/pot/` 아래입니다.

| 파일 | 구분 | 내용 |
|---|---|---|
| `service/PotMemberService.java` | 수정 | 도착 확인(`confirmArrival`), 내보내기(`kick`) 구현. 나머지 메서드(참여·취소·좌석)는 그대로 스텁 |
| `service/PotJobService.java` | 수정 | 스케줄러 1·2·3·4·11 — '어느 방을 처리할지' 고르고 방마다 Processor 호출 |
| `service/PotJobProcessor.java` | 새 파일 | 스케줄러 1·2·3·11 — '방 하나 처리'. 메서드 하나 = 트랜잭션 하나 = 방 하나 |
| `service/PotExitHandler.java` | 새 파일 | 퇴장·환불·해산 공통 처리 (KICKED, NO_SHOW, 방 해산, 입장 요청 취소) |
| `repository/TaxipotRepository.java` | 수정 | 스케줄러 대상 방 id 조회 쿼리 4개 |
| `repository/TaxipotMemberRepository.java` | 수정 | 참여자 member_id 조회, 지각 경고 대상 조회 쿼리 2개 |

`ErrorCode`, `AppProperties`, DTO 등 다른 담당의 파일과 `global/`은 수정하지 않았습니다. 필요한 에러 코드·설정값은 모두 이미 들어 있는 것을 썼습니다.

## 2. 구조

```
TaxipotScheduler (언제)  →  PotJobService (어느 방)  →  PotJobProcessor (방 하나, 트랜잭션 1개)
                                                              ↓
도착 확인·내보내기 API (PotMemberService)  ─────────→  PotExitHandler (퇴장·환불 공통 창구)
```

`PotJobService`와 `PotJobProcessor`를 나눈 이유는 `CONVENTIONS.md` 8장 그대로입니다. 같은 클래스 안에서 `this.`로 부르면 `@Transactional`이 적용되지 않으므로, '방 목록 돌기'와 '방 하나 처리'를 다른 빈으로 분리했습니다.

`PotExitHandler`는 '퇴실 정산 창구'입니다. 내보내기 API, 스케줄러 1·2·3이 모두 같은 퇴장·환불 규칙을 쓰므로 한곳에 모았습니다. 참여 취소·방 취소 API를 구현할 때도 `refundHold`, `dissolve`를 그대로 쓰면 환불 규칙이 한 벌로 유지됩니다. 모든 메서드는 `Propagation.MANDATORY`라 호출 측 트랜잭션 안에서, 락을 잡은 상태로만 불러야 합니다.

## 3. API

### 도착 확인 `POST /api/pots/{potId}/members/me/arrival`

락은 회원(USERS → WALLET) → TAXIPOT 순서로 잡습니다. 지갑은 바꾸지 않지만 스케줄러 1과 같은 사람을 두고 경쟁하므로 같은 순서로 잠가야 데드락이 나지 않습니다. 검증은 락을 잡은 뒤에 아래 순서로 합니다.

| 순서 | 조건 | 에러 |
|---|---|---|
| 1 | 방이 있는가 | `404` (`Errors.notFound`) |
| 2 | 이 방의 JOINED 참여자인가 | `403 NOT_POT_MEMBER` |
| 3 | 방이 RECRUITING·CLOSED 인가 | `409 INVALID_POT_STATUS` (명세에 없음, 6장) |
| 4 | 아직 도착 확인 전인가 | `409 ALREADY_ARRIVED` |
| 5 | now < 집결 기한 인가 (정각부터 불가) | `409 ARRIVAL_DEADLINE_PASSED` |
| 6 | 출발지에서 50m 이내인가 (하버사인, 서버가 최종 판정) | `400 OUT_OF_ARRIVAL_RANGE` (detail: 거리·허용 반경) |

통과하면 `arrived_at = now`로 기록하고, 남은 JOINED 기준으로 도착 인원을 셉니다. 내가 마지막 도착자이고 참여자가 2명 이상이면 전원에게 "모두 도착했어요" POT 알림을 한 번 보냅니다. 마지막으로 `potUpdated`를 방송하고 `ArrivalResponse`(도착 시각, 거리, 도착 인원, 전체 인원, 전원 도착 여부)를 돌려줍니다.

### 내보내기 `POST /api/pots/{potId}/members/{userId}/kick`

락은 방장이 아니라 '대상 회원'의 USERS → WALLET → TAXIPOT입니다(`CONVENTIONS.md` 8장). 대상의 지갑(환불)과 정지 상태를 바꾸기 때문입니다.

| 순서 | 조건 | 에러 |
|---|---|---|
| 0 | 대상이 본인이 아닌가 (락 전에 검사) | `NOT_POT_MEMBER` (명세 409, 코드 403 — 6장) |
| 1 | 요청자가 방장인가 | `403 NOT_POT_HOST` |
| 2 | 방이 RECRUITING·CLOSED 인가 | `409 INVALID_POT_STATUS` |
| 3 | 대상이 이 방의 JOINED 참여자인가 | `403 NOT_POT_MEMBER` |
| 4 | 대상이 아직 도착 전인가 | `409 MEMBER_ALREADY_ARRIVED` |

처리는 집결 기한을 기준으로 갈립니다. 기한 전이면 KICKED(페널티 없음, 전액 환불)이고, 기한 정각부터는 NO_SHOW(노쇼 +1, 15분 정지, 전액 환불)입니다. 기한이 지났는데 스케줄러가 아직 돌기 전 몇십 초 사이에 방장이 먼저 내보내도 결과가 스케줄러 1과 똑같아지도록 경계를 맞췄습니다. 두 경우 모두 인원 −1, `has_canceled = 1`, CLOSED였다면 RECRUITING, 대상에게 KICKED 알림을 보냅니다.

## 4. 스케줄러

주기와 on/off는 기존 `TaxipotScheduler`와 `app.scheduler.*` 설정을 그대로 씁니다.

| 번호 | 주기 | 대상 | 처리 |
|---|---|---|---|
| 1 미도착 자동 퇴장 | 1분 | RECRUITING·CLOSED, 집결 기한 ≤ now, 미도착 JOINED가 있고 인원 2명 이상 | 방장 도착: 미도착 동승자만 각각 노쇼 퇴장. 방장 미도착: 방 해산, 방장만 노쇼, 나머지 전액 환불, 입장 요청 취소, POT 알림 |
| 2 방장 단독 방 해산 | 1분 | RECRUITING, 인원 1명, 출발 시각 ≤ now | 해산, 방장 전액 환불(정지 없음), 입장 요청 취소, POT 알림 |
| 3 미출발 방 자동 취소 | 1분 | RECRUITING·CLOSED, 출발 시각 + 30분 ≤ now | 취소, 전원 환불(정지 없음), 입장 요청 취소, POT 알림 |
| 4 지각 경고 | 1분 | now < 집결 기한 ≤ now + 3분, JOINED 미도착, LATE_WARNING을 받은 적 없음 | LATE_WARNING 알림 1회 |
| 11 출발 확인 알림 | 30초 | RECRUITING·CLOSED, `start_reminded_at` NULL, JOINED 2명 이상 전원 도착, 마지막 도착 + 3분 ≤ now | 방장에게 POT 알림 "모두 모였어요. 출발했다면 탑승 시작 버튼을 눌러주세요.", `start_reminded_at = now` |

방 해산(스케줄러 1·2·3)은 방 상태만 CANCELED로 바꾸고, 참여자 행의 status·`current_members`·`has_canceled`는 바꾸지 않습니다. 명세의 방 취소도 'status = CANCELED + 환불'로만 표현하고, CANCELED 방은 '진행 중인 방' 조회에서 빠져 다른 방에 바로 참여할 수 있기 때문입니다. 입장 요청 대기자(REQUESTED)는 수락 시점에 예치하므로 환불 없이 CANCELED만 합니다.

쿼리에서 `departure_time + 30분 ≤ now`는 `departure_time ≤ now − 30분`으로 바꿔 넘깁니다. 컬럼에 계산을 걸지 않아야 인덱스를 탈 수 있습니다. 스케줄러 4의 중복 방지는 별도 컬럼 대신 'LATE_WARNING 알림이 이미 있는가'로 판단합니다. `fixedDelay`라 실행 간격이 정확히 60초가 아니어서 시간창 방식은 빠뜨리거나 두 번 보낼 수 있고, 컬럼 추가는 DB 설계서 변경이 필요하기 때문입니다.

## 5. 동시성 (이중 환불 방지)

스케줄러 1·2·3은 모두 같은 모양입니다. 은행 창구에 비유하면 "번호표 뽑기 → 창구 앞에서 서류 다시 확인 → 처리"입니다.

1. 대상 방 id를 트랜잭션·락 없이 읽습니다 (`PotJobService`).
2. 방마다 `PotJobProcessor`가 새 트랜잭션을 열고, 그 방의 JOINED 회원을 `lockAll`(id 오름차순, USERS → WALLET)로 잠근 뒤 TAXIPOT을 잠급니다.
3. 락을 잡은 뒤 조건을 다시 확인합니다. 목록을 읽은 사이 방장이 탑승 시작을 눌렀거나 API가 먼저 취소했으면 아무것도 하지 않고 끝냅니다.
4. 참여자를 다시 읽어서, 2단계에서 잠그지 못한 회원이 끼어들었으면(그 사이 입장 요청 수락 등) 이번 실행은 건너뜁니다. TAXIPOT을 잡은 상태에서 회원을 추가로 잠그면 락 순서가 뒤집혀 데드락이 날 수 있기 때문입니다. 아무것도 바꾸지 않고 끝나므로 1분 뒤 다음 실행이 새 목록으로 처리합니다.
5. 처리 → 알림 → `potUpdated`(커밋 후 방송).

스케줄러 11은 지갑을 바꾸지 않으므로 TAXIPOT만 잠급니다. TAXIPOT은 락 순서의 마지막이라 이것만 잡는 것은 데드락을 만들지 않습니다.

환불(`PotExitHandler.refundHold`)은 정산 행이 HELD일 때만 합니다. 이미 REFUNDED·SETTLED이거나 행이 없으면 경고 로그만 남기고 0을 돌려줍니다. 이 검사가 락을 잡은 뒤 실행되므로, API와 스케줄러가 같은 사람을 동시에 처리해도 두 번째 쪽은 첫 번째 커밋 결과를 보고 건너뜁니다.

방 하나에서 예외가 나면 그 방만 롤백되고 ERROR 로그(스택 포함)를 남긴 뒤 다음 방을 계속 처리합니다. 실패한 방은 상태가 그대로 남아 다음 실행에 다시 대상이 됩니다. `CONVENTIONS.md` 9장의 '예외를 삼키지 않는다'와 충돌하지 않도록 반드시 로그를 남깁니다.

## 6. 명세에 없어 임의로 정한 것 (팀 확정 필요)

1. **혼자 남은 방장의 방**: 집결 기한에 스케줄러 1이 처리하지 않고(인원 2명 이상만 대상), 출발 시각에 스케줄러 2가 정지 없이 해산합니다. 명세 2번은 "출발 시각 전까지는 입장 요청 수락으로 인원을 채울 수 있음"인데, 집결 기한(출발 5분 전)에 1번이 먼저 돌면 혼자 기다리던 방장이 노쇼 정지를 받고 방이 사라져 2번의 '정지 없음' 규칙과 충돌하기 때문입니다.
2. **방장 미도착 해산 시 미도착 동승자**: 노쇼 처리하지 않고 전액 환불만 합니다. 명세는 '방장도 같은 노쇼 처리'만 적었고, 방이 해산되면 남아서 기다린 사람과 안 온 사람을 구분할 의미가 없어서입니다. 바꾸려면 `PotJobProcessor.processNoShow`의 방장 미도착 분기만 고치면 됩니다.
3. **자기 자신 내보내기 응답 코드**: 명세는 409 `NOT_POT_MEMBER`인데 `ErrorCode.NOT_POT_MEMBER`는 403으로 정의돼 있습니다. 공통 담당과 확인이 필요합니다.
4. **지각 경고 N분**: 명세에 값이 없어 3분으로 가정했습니다 (`app.pot.late-warning-minutes`, `CONVENTIONS.md` 15장과 동일).
5. **도착 확인 시 방 상태 검증**: 명세 에러 목록에는 없지만, 취소됐거나 운행 중인 방에 도착을 기록하면 안 되므로 `409 INVALID_POT_STATUS`를 추가했습니다.

## 7. 확인 방법

`backend`에서 `.\gradlew.bat compileJava`로 컴파일이 통과하는 것을 확인했습니다. 새 JPQL 쿼리 6개는 컴파일 단계가 아니라 서버를 띄울 때 검사되므로, 서버를 한 번 실행해 에러 없이 뜨는지 확인해야 합니다.

스케줄러는 기본 설정(`application.yml`)에서 `app.scheduler.enabled: false`라 돌지 않습니다. 실제로 돌려 보려면 demo 프로필(`application-demo.yml`, `enabled: true`)로 실행합니다. 이때 아직 구현되지 않은 다른 담당의 스케줄러 작업은 `NotImplementedYetException` 로그가 찍힐 수 있습니다.
