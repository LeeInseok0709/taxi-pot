# 택시팟 서버 개발 규칙

이 문서는 백엔드 네 명이 서로 기다리지 않고 동시에 개발하고, 합칠 때 충돌이 나지 않게 하기 위한 규칙입니다. 기준 문서는 API 명세서 3.9.0(`src/main/resources/static/api-spec/taxipot-api.yaml`)과 DB 설계서 13차이며, 둘이 다르면 DB 설계서를 따릅니다(명세 첫 줄). 이 문서와 명세가 다르면 명세가 맞고, 이 문서를 고칩니다.

## 1. 담당

담당은 패키지 단위로 나눴습니다. 자기 담당이 아닌 패키지의 파일은 수정하지 않고, 필요하면 담당자에게 요청합니다. 각 서비스 파일 맨 위 주석과 `TODO(담당)`에도 같은 이름이 적혀 있습니다.

| 담당 | 이름 | 패키지·클래스 | 상태 |
|---|---|---|---|
| 공통·인증 | (이름) | `global/**`, `domain/auth`, `domain/verification`의 `VerificationService`, `domain/notification`, `domain/university`, `domain/scheduler`, `domain/admin/bootstrap`, `tools/` | 구현 완료 (유지보수) |
| 택시팟 | (이름) | `domain/pot/**`(목록·생성·상세·수정·취소·탑승 시작, 참여·취소·좌석·도착·내보내기, 입장 요청, 내 이용 기록, 운영자 방 목록, `PotJobService`), `domain/chat/**` | 스텁 |
| 지갑·정산 | (이름) | `domain/wallet/**`(조회·내역·미수금·출금·운영자 잔액 조정), `domain/payment/**`, `domain/settlement/**`(실요금 청구·정산 조회·영수증·운영자 청구 심사·장애 복구 일괄 입력, `SettlementJobService`) | 스텁 |
| 회원·운영 | (이름) | `domain/user/**`(내 정보·수정·탈퇴·비밀번호·계좌·FCM·공개 프로필·운영자 회원 관리, `MemberJobService`), `domain/verification`의 `AdminVerificationService`, `domain/report/**`, `domain/rating/**`(`RatingJobService`), `domain/admin`의 `StatisticsService` | 스텁 |

`global/`의 `ErrorCode`, `AppProperties`, `SecurityConfig`, `DebtRestrictionInterceptor`는 여러 사람이 동시에 고치면 충돌이 가장 많이 나는 파일이라 공통·인증 담당만 수정합니다. 에러 코드 77개는 이미 전부 들어 있습니다.

## 2. 꼭 지킬 것 (요약)

1. DTO는 직접 만들거나 고치지 않습니다. 명세에서 자동 생성한 record를 그대로 씁니다(4장).
2. 로그인 회원은 컨트롤러의 `@LoginUserId Long userId`로만 받습니다. 서비스에서 SecurityContext를 직접 읽지 않습니다.
3. 예외는 `BusinessException`만 던집니다. detail이 필요한 코드(`INSUFFICIENT_BALANCE` 등 15개)는 반드시 `Errors.xxx()`로 만듭니다(9장).
4. '지금'은 `timeProvider.now()`로 구합니다. `LocalDateTime.now()`를 직접 쓰지 않습니다(10장).
5. 지갑 금액은 `WalletLedger`로만 바꿉니다. 원장(POINT_HISTORY) 기록이 함께 남습니다(7장).
6. 지갑·참여 상태를 바꾸는 트랜잭션은 `MemberWalletLocker`로 USERS → WALLET을 잠근 뒤 TAXIPOT을 잠급니다(8장).
7. 다른 도메인 Entity를 `@ManyToOne`으로 참조하지 않습니다. FK는 `Long potId`처럼 id로만 둡니다(6장).
8. 서비스끼리는 5장의 의존 방향으로만 호출합니다. 반대 방향이 필요하면 Facade를 만듭니다.
9. 명세에 '서버 설정값'으로 적힌 숫자는 `AppProperties`에서 읽습니다.
10. 미수금 이용 제한은 인터셉터가 처리하므로 각 API에서 따로 검사하지 않습니다.

## 3. 구조

요청은 Controller → Service → Repository 순서로 흐릅니다. 컨트롤러는 명세에서 자동 생성했고 `@Valid` 검증과 서비스 호출만 합니다. 로직은 전부 서비스에 둡니다. 컨트롤러의 경로·파라미터가 명세와 다르다고 생각되면 컨트롤러를 고치지 말고 명세부터 고친 뒤 재생성합니다.

서비스 메서드 위의 주석은 명세 3.9.0의 설명과 에러 코드를 그대로 옮긴 것입니다. 이 주석대로 구현하고, 다 만들면 `throw new NotImplementedYetException(...)` 줄을 지웁니다. 배포 전에는 프로젝트 전체에서 `NotImplementedYetException`을 검색해 0개인지 확인합니다.

성공 응답은 명세대로 데이터를 감싸지 않고 바로 반환합니다. 본문이 없는 API는 서비스가 `void`를 반환하고, 컨트롤러가 `204 No Content`로 응답하도록 이미 생성되어 있습니다. 이미지 조회 API(캡처본·영수증)는 서비스가 `FileStorage.load(path)`의 결과(`StoredFile`)를 반환하면 컨트롤러가 이미지 바이너리로 내보냅니다.

## 4. DTO

`domain/*/dto/`의 record 81개와 `global/code/`의 enum 22개는 `tools/gen_dto.py`가 명세 yaml에서 만든 파일입니다. 필수 여부, 길이·패턴·범위 제약, 기본값(`maxMembers=4`, `agreeMarketing=false`)이 명세 그대로 들어 있습니다. 명세가 바뀌면 다음처럼 다시 생성하고, 바뀐 DTO 때문에 컴파일이 깨지는 곳을 고칩니다.

```bash
# 명세 yaml 을 src/main/resources/static/api-spec/taxipot-api.yaml 에 덮어쓴 뒤
python3 tools/gen_dto.py src/main/resources/static/api-spec/taxipot-api.yaml src/main/java/com/taxipot tools/code-definitions.txt
python3 tools/gen_api.py src/main/resources/static/api-spec/taxipot-api.yaml src/main/java/com/taxipot
```
`gen_api.py`는 컨트롤러와 '아직 손대지 않은' 서비스 스텁을 다시 만들므로, 구현을 시작한 서비스가 덮어써지지 않게 재생성은 공통·인증 담당이 하고 결과를 확인한 뒤 커밋합니다.

DTO를 record로 둔 이유는 Kotlin 앱과의 연동 때문입니다. 클래스에 Lombok `@Getter`를 쓰면 `isRated` 같은 boolean이 JSON에서 `"rated"`로 나가 앱에서 항상 false가 됩니다(명세 3.2.0). record는 이름을 그대로 쓰고, 생성기가 `is`로 시작하는 필드에 `@JsonProperty`도 붙였습니다.

응답 DTO의 일시는 `OffsetDateTime`입니다. Entity의 `LocalDateTime`은 `Kst.toOffset(entity.getCreatedAt())`으로 바꿉니다(null이면 null). 요청으로 받은 일시는 `Kst.toLocal(request.departureTime())`으로 Entity에 넣습니다. `DECIMAL` 컬럼(평점·좌표)은 Entity에서 `BigDecimal`이고 응답에서는 `.doubleValue()`로 숫자를 내보냅니다. 응답의 null 필드는 키를 생략하지 않고 null로 내려가야 하므로 `@JsonInclude(NON_NULL)`을 쓰지 않습니다.

## 5. 서비스 의존 방향

서비스가 서로를 주입하면(예: 회원 → 지갑 → 회원) Spring이 아예 시작되지 않습니다. 그래서 아래 방향으로만 호출합니다. 왼쪽이 오른쪽을 호출할 수 있습니다.

| 도메인 서비스 | 호출할 수 있는 도메인 서비스·공용 컴포넌트 |
|---|---|
| notification | (없음) |
| user | notification |
| wallet, payment | user, notification |
| pot, chat | wallet, user, notification |
| settlement | pot, wallet, user, notification |
| rating, report | settlement, pot, user, notification |
| verification, auth | user, wallet, notification |
| admin(통계), scheduler, `*Facade` | 전부 |

반대 방향이 필요하면 `*Facade` 클래스를 만들어 그 안에서 여러 서비스를 부릅니다. 예를 들어 회원 탈퇴(`UserWithdrawFacade`)는 지갑 잔액 소멸, 결제 대기 충전 취소, 참여 중인 방 확인을 함께 하므로 Facade로 만들어 두었습니다. 서비스가 Facade를 부르는 것은 금지입니다.

Repository는 다른 도메인 것이라도 주입해서 써도 됩니다. Repository는 서비스를 부르지 않으므로 순환이 생기지 않습니다. 예를 들어 택시팟 참여 시 SETTLEMENT 행을 만들 때는 `SettlementRepository`와 `Settlement.hold()`를 직접 쓰면 됩니다. 다만 그 Entity의 상태 변경은 Entity에 미리 만들어 둔 메서드(`hold`, `rehold`, `refund`, `settle`, `payDebt` 등)로만 합니다. 필요한 메서드가 없으면 그 Entity 담당에게 요청합니다.

## 6. Entity 규칙

Entity 14개는 MySQL 8 실제 DB에서 `ddl-auto: validate`로 확인을 마쳤습니다. 컬럼을 추가해야 하면 DB 설계서 → `db/schema.sql` → Entity 순서로 바꾸고, 서버가 validate를 통과하는지 확인합니다. Hibernate 6은 아래 타입을 기본 매핑과 다르게 보기 때문에, 새 필드를 만들 때 반드시 이 표를 따릅니다. 틀리면 서버가 뜨지 않습니다.

| DB 컬럼 | Java 필드 | 붙일 어노테이션 |
|---|---|---|
| `VARCHAR` 코드값 (status 등) | enum | `@Enumerated(EnumType.STRING)` `@JdbcTypeCode(SqlTypes.VARCHAR)` |
| `CHAR(1)` (gender) | enum | `@Enumerated(EnumType.STRING)` `@JdbcTypeCode(SqlTypes.CHAR)` |
| `TINYINT` (인원·별점·횟수) | `Integer` | `@JdbcTypeCode(SqlTypes.TINYINT)` |
| `TINYINT(1)` (is_xxx, has_xxx) | `Boolean` | 없음 |
| `DECIMAL` (좌표·평점) | `BigDecimal` | `@Column(precision, scale)` — `Double` 금지 |
| 계산 컬럼 (`RATING.avg_score`) | `BigDecimal` | `@Column(insertable = false, updatable = false)` |
| 암호화 컬럼 (phone, account_no) | `String`(평문) | `@Convert(converter = EncryptedStringConverter.class)` |

모든 필드에 `@Column(name = "snake_case")`를 적습니다. 테이블 이름이 대문자(`USERS`)라 이름 변환 규칙을 끄고(`PhysicalNamingStrategyStandardImpl`) 이름을 그대로 쓰기 때문입니다. `created_at`과 `updated_at`이 둘 다 있는 테이블은 USERS와 TAXIPOT뿐이라 이 둘만 `BaseTimeEntity`를 상속합니다. 나머지는 컬럼 이름이 달라서(`joined_at`, `sent_at`, `requested_at`, WALLET은 `updated_at`만 있음) 각 Entity에서 `@PrePersist`로 채웁니다.

Entity에는 `@Setter`를 쓰지 않고, 상태는 의도가 드러나는 메서드(`pot.start(now)`, `member.markNoShow(now)`, `user.suspend(until)`)로 바꿉니다. 이 메서드들은 DB CHECK 제약을 지키도록 관련 컬럼을 함께 바꿉니다. '바꿔도 되는 상태인가'(예: RECRUITING이 아니면 409)는 서비스에서 먼저 검사합니다.

## 7. 공용 컴포넌트 (이미 구현됨, 그대로 호출)

아래 메서드는 시그니처가 확정되어 있으므로 서로 기다리지 않고 바로 호출해서 개발하면 됩니다.

| 클래스 | 메서드 | 용도 |
|---|---|---|
| `MemberWalletLocker` | `lock(memberId)` → `LockedMember(user, wallet)`, `lockAll(ids)` | USERS → WALLET 락 (8장) |
| `WalletLedger` | `open`, `hold(wallet, potId, amount)`, `refundHold(...)`, `credit(wallet, type, amount, potId, paymentId, withdrawalId, checkLimit)`, `debit(wallet, type, amount, potId, withdrawalId)` | 지갑 금액 변경 + 원장 기록. `hold`는 잔액 부족 시 `INSUFFICIENT_BALANCE`, `credit(checkLimit=true)`는 상한 초과 시 `WALLET_LIMIT_EXCEEDED` |
| `PotPolicy` | `holdAmount(expectedFare, maxMembers)`, `arrivalDeadline(departure)`, `cancelDeadline(departure)`, `minDepartureTime(now)`, `maxDepartureTime(now)`, `arrivalRadiusMeters()`, `feeAmount()` | 예치액 = floor(예상요금 × 1.2 ÷ 인원) + 1,000 등 계산 |
| `PotEligibilityValidator` | `validate(user, wallet, pot, requestedGender, holdAmount)` | 방 생성·참여·입장 요청 공통 이용 조건을 명세 순서대로 검사 |
| `NotificationSender` | `send(memberId, potId, type, title, content)`, `sendAll(...)`, `sendToAdmins(...)` | 알림 저장 + 커밋 후 푸시 |
| `PotRealtimePublisher` | `potUpdated(potId)`, `chatMessage(potId, response)` | STOMP `/topic/pots/{potId}` 방송 (커밋 후) |
| `User` | `suspend(until)`(null = 영구, 더 긴 쪽 유지), `releaseSuspension()`, `isSuspendedAt(now)`, `increaseNoShowCount()`, `increaseReportCount()`, `increaseCleanStreak()`, `changeRating(delta)`, `withdraw(now)`, `changePassword(hash)`(token_version + 1) | 정지 규칙 통일 등 |
| `AuthService` | `tokens(user)` | 비밀번호 변경 후 새 `TokenResponse` |
| `UserMapper` | `toMyProfile(user)`, `nextSuspensionIn(user)` | 내 정보 응답 |
| `VerificationService` | `toResponse(verification)` | 인증 응답 (운영자 심사에서도 사용) |
| `FileStorage` | `saveImage(file, dir)`, `load(path)`, `delete(path)`, `deleteAfterCommit(path)` | jpg·png 저장. 트랜잭션이 롤백되면 저장한 파일 자동 삭제 |
| `Errors` | `insufficientBalance`, `suspended`, `potFull`, `joinDeadlinePassed` 등 15개 + `notFound`, `invalidInput` | detail 규격이 정해진 예외 |
| `Paging`, `PageMeta` | `Paging.of(page, size, sort)`, `PageMeta.from(page)` | 목록 API (11장) |
| `TimeProvider`, `Kst` | `now()`, `toOffset()`, `toLocal()` | 시각 (10장) |
| `GeoUtils`, `Texts` | `distanceMeters(...)`, `digitsOnly`, `maskAccount`, `isReservedNickname` | 거리·문자열 |
| `BankCatalog`, `QuickBlockCatalog` | `exists(code)`, `findByContent(content)` | 서버 상수 목록 |

같은 담당 안에서 쓰는 공용 메서드(예: 지갑·정산 담당의 '미수금 자동 납부'를 결제 승인과 운영자 조정에서 함께 쓰는 것)는 담당자가 자기 패키지에 만들면 됩니다. 다른 담당이 불러야 하는 메서드가 새로 필요하면, 먼저 시그니처와 빈 몸체(`NotImplementedYetException`)만 올려서 상대가 컴파일할 수 있게 한 뒤 구현합니다.

## 8. 트랜잭션과 락

지갑이나 참여 상태를 바꾸는 서비스 메서드는 `@Transactional`이고, 락은 반드시 USERS → WALLET → TAXIPOT 순서로 잡습니다. 순서가 섞이면 동시에 요청이 들어왔을 때 데드락이 납니다. `MemberWalletLocker`가 앞의 두 단계를 대신하므로 아래 모양을 그대로 쓰면 됩니다.

```java
@Transactional
public JoinResponse join(Long userId, Long potId, JoinRequest request) {
    LockedMember me = memberWalletLocker.lock(userId);                     // 1. USERS 2. WALLET
    Taxipot pot = taxipotRepository.findByIdForUpdate(potId)              // 3. TAXIPOT
            .orElseThrow(() -> Errors.notFound("택시팟"));
    int hold = potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers());
    eligibilityValidator.validate(me.user(), me.wallet(), pot, null, hold);
    // ... 상태 검사, TAXIPOT_MEMBER·SETTLEMENT 저장
    walletLedger.hold(me.wallet(), potId, hold);
    pot.addMember();
    potRealtimePublisher.potUpdated(potId);
    return ...;
}
```

방장이 다른 회원을 처리하는 작업(내보내기, 입장 요청 수락)은 방장이 아니라 '대상 회원' id로 `lock`합니다. 여러 회원을 다루는 작업(정산, 방 취소, 스케줄러)은 `lockAll(memberIds)`로 회원을 id 오름차순으로 모두 잠근 뒤 TAXIPOT을 잠급니다. 정원 초과와 잔액 부족은 락을 잡은 뒤에 다시 검사합니다.

스케줄러 작업은 '대상 방 id 목록 조회'를 트랜잭션 밖에서 하고, 방마다 별도의 `@Transactional` 메서드를 호출합니다. 그 안에서 TAXIPOT을 잠근 뒤 상태를 다시 확인해야 API와 동시에 실행돼도 이중 환불이 생기지 않습니다. 같은 클래스 안의 `@Transactional` 메서드를 `this.`로 부르면 트랜잭션이 적용되지 않으니, 방 단위 처리 메서드는 다른 빈에 두거나 별도 클래스로 분리합니다.

## 9. 에러 처리

서비스에서는 `throw new BusinessException(ErrorCode.NOT_POT_HOST)`처럼 던지기만 하고, 응답 변환은 `GlobalExceptionHandler`가 합니다. 문구에 값이 들어가야 하면 `new BusinessException(code, "메시지")`를 씁니다. 명세가 detail 필드를 정해 둔 15개 코드(INSUFFICIENT_BALANCE, SUSPENDED_USER, OUT_OF_ARRIVAL_RANGE, CANCEL_TIME_EXPIRED, ARRIVAL_DEADLINE_PASSED, ACTIVE_POT_EXISTS, GENDER_RESTRICTED, POT_FULL, NOT_VERIFIED, WALLET_LIMIT_EXCEEDED, INVALID_DATETIME_FORMAT, INVALID_EMAIL_CODE, JOIN_DEADLINE_PASSED, DEPARTURE_TIME_TOO_SOON/FAR, UNPAID_DEBT_EXISTS)는 반드시 `Errors`의 메서드로 만듭니다. detail 이름이 하나라도 다르면 앱 팝업 문구가 깨집니다.

`try { } catch (Exception e)`로 예외를 삼키지 않습니다. 모르는 예외는 500 `INTERNAL_ERROR`로 나가고 로그가 남아야 원인을 찾을 수 있습니다. DB UNIQUE 위반(동시 가입 등)은 `DbConstraintTranslator`가 `DUPLICATE_EMAIL` 같은 명세 코드로 바꿔 줍니다. 새 UNIQUE 키를 만들면 그 파일의 표에 추가를 요청하세요. DB CHECK 위반은 409로 바뀌지만 ERROR 로그가 남습니다. 서비스 검증이 빠졌다는 뜻이므로 로그를 보면 검증을 추가합니다.

명세에 없는 서버 전용 코드는 `METHOD_NOT_ALLOWED`(405), `INTERNAL_ERROR`(500), `NOT_IMPLEMENTED`(501) 세 개입니다. `ErrorCodeSpecTest`가 나머지 77개를 명세와 비교하므로 ErrorCode를 고치면 테스트로 확인합니다.

## 10. 일시

모든 일시는 KST입니다. Entity는 `LocalDateTime`, DTO는 `OffsetDateTime`이고 응답은 항상 `2026-10-16T23:30:00+09:00` 형식으로 나갑니다(초 미만 없음). 요청 본문이나 쿼리 파라미터에 오프셋이 없거나 `Z`면 서버가 자동으로 400 `INVALID_DATETIME_FORMAT`(detail.field, detail.value)을 응답하므로, 서비스에서는 형식 검사를 하지 않습니다. 쿼리 파라미터에서 `+`가 공백으로 깨진 값(`...T23:00:00 09:00`)도 서버가 복구합니다.

서비스에서 현재 시각은 `timeProvider.now()`로 구합니다. 초 단위로 잘려 있어 DB에 저장된 값과 비교해도 어긋나지 않고, 테스트에서 시간을 고정할 수 있습니다. 정지 여부는 `user.isSuspendedAt(now)`로 봅니다. 정지 기간이 지났는데 스케줄러가 아직 ACTIVE로 바꾸지 않은 1분 사이에도 정지로 판단하지 않기 위해서입니다.

## 11. 목록 API

컨트롤러는 `page`·`size`를 int로 받고(기본 0·20), 서비스가 `Paging.of(page, size, Sort...)`로 PageRequest를 만듭니다. Spring의 `Pageable` 파라미터는 쓰지 않습니다. 명세의 `sort=DEPARTURE_TIME` 같은 값을 Entity 필드명으로 해석하다 500이 나고 size 상한도 없기 때문입니다. 정렬은 서비스가 `Sort.by(...)`로 직접 정합니다. 응답은 `new XxxPageResponse(page.map(this::toResponse).getContent(), PageMeta.from(page))` 모양이고, `NotificationService`가 완성된 예시입니다. 조건이 많은 목록(택시팟 목록)은 `TaxipotRepository`가 `JpaSpecificationExecutor`를 상속하고 있으니 Specification으로 조건을 조합하면 됩니다. 채팅만 커서 방식이고 `ChatMessageRepository`에 조회 메서드가 있습니다.

## 12. 업로드와 이미지

캡처본·영수증 업로드는 `fileStorage.saveImage(request.receiptImage(), FileStorage.DIR_RECEIPT + "/" + potId)`처럼 호출하고, 반환된 상대 경로를 Entity에 저장합니다. jpg·png 검사(확장자가 아니라 파일 앞부분 시그니처), 10MB 제한, 롤백 시 파일 삭제가 모두 들어 있습니다. 재청구처럼 기존 파일을 바꿀 때는 이전 경로를 `deleteAfterCommit`으로 지웁니다. 이미지 조회는 `return fileStorage.load(pot.getReceiptImgPath());`만 하면 됩니다. 경로가 null이거나 파일이 없으면 404가 나갑니다.

## 13. 알림과 실시간

알림은 `notificationSender.send(...)`를 호출하면 NOTIFICATION 행이 같은 트랜잭션에 저장되고, FCM 푸시는 커밋된 뒤에만 나갑니다. 지금은 FCM 대신 로그만 찍는 `LogPushSender`가 동작하며, FCM을 붙일 때는 `PushSender`를 구현한 빈을 하나 추가하면 자동으로 바뀝니다.

방 화면 갱신은 `potRealtimePublisher.potUpdated(potId)`를 부르면 `/topic/pots/{potId}` 구독자에게 `{type: "POT_UPDATED", potId, payload}`가 갑니다. 인원·상태·도착·입장 요청이 바뀌는 모든 API와 스케줄러에서 호출합니다. 채팅은 REST로 저장한 뒤 `chatMessage(potId, response)`로 방송합니다. 앱이 STOMP로 직접 보내는(SEND) 것은 막아 두었습니다. STOMP 연결은 `Authorization: Bearer` 헤더로 인증하고, 구독은 그 방의 JOINED·BOARDED 참여자이면서 미수금이 없을 때만 허용됩니다.

## 14. 깃

`main`에는 직접 푸시하지 않고, 각자 `feat/pot-join`처럼 기능 브랜치에서 작업한 뒤 PR로 합칩니다. PR을 올리기 전에 `main`을 받아 합치고, 서버가 뜨는지(validate 통과)와 해당 API를 Swagger로 한 번 호출해 봤는지 확인합니다. 커밋 메시지는 `feat(pot): 택시팟 참여 구현`, `fix(wallet): 출금 잔액 검사`처럼 씁니다.

`.gitattributes`와 `.editorconfig`가 줄바꿈(LF)과 import 정리 방식을 통일하므로, IntelliJ에서 `Settings > Editor > Code Style`의 'Enable EditorConfig support'가 켜져 있는지 확인합니다. Windows에서 처음 받을 때 줄바꿈 경고가 나오면 `git config --global core.autocrlf false`를 한 번 실행합니다. 와일드카드 import(`import java.util.*`)는 쓰지 않습니다. 비밀번호·키를 담은 설정 파일과 `uploads/` 폴더는 커밋하지 않습니다.

## 15. 명세에 값이 없어 틀에서 정한 것 (팀 확정 필요)

아래는 명세에 '서버 설정값'·'서버 상수'라고만 되어 있거나 값이 적혀 있지 않아, 틀을 만들며 정한 것입니다. 팀이 확정하면 명세서에 적고 코드·설정을 맞춥니다. 이 목록에 없는 숫자·문구는 모두 명세나 DB 설계서에 근거합니다.

| 항목 | 지금 값 | 위치 |
|---|---|---|
| 지각 경고 시점 (명세: 'N은 서버 설정값') | 집결 기한 3분 전 | `app.pot.late-warning-minutes` |
| 시연 프로필의 단축 시간 (명세: '시연용 프로필에서 짧게 줄인다') | 정하지 않음 (명세 값 그대로) | `application-demo.yml` 주석 |
| 은행 목록 (명세: '서버 상수') | 6개(국민·농협·신한·우리·카카오뱅크·토스뱅크), 금융결제원 은행 코드 | `BankCatalog` |
| 퀵블록 code 값 (명세는 문구만 있음) | `LATE_1`~`LATE_5`, `ARRIVED` | `QuickBlockCatalog` |
| 이메일 인증코드 자릿수 (DB: VARCHAR(10)) | 숫자 6자리 | `app.verification.email-code-length` |
| 실시간 `POT_UPDATED` payload | `{potId}` | `PotRealtimePublisher` |
| 서버 전용 에러 코드 | 405 METHOD_NOT_ALLOWED, 500 INTERNAL_ERROR, 501 NOT_IMPLEMENTED | `ErrorCode` |
| 운영자 계정 생성 방식 (명세: 운영자 계정 1개) | 서버 시작 시 `app.admin.*` 로 자동 생성 | `AdminAccountInitializer` |

에러·알림 문구에 설정값(분·시간·금액)을 숫자로 직접 쓰지 않습니다. `Errors`의 메서드에 설정값을 넘겨 문구를 만들어야, 시연 프로필에서 값을 바꿔도 문구가 함께 바뀝니다. 예를 들어 취소 마감은 `Errors.cancelTimeExpired(potPolicy.cancelDeadline(departure), potPolicy.cancelDeadlineMinutes())`처럼 씁니다.

## 16. API 하나를 끝냈다는 기준

서비스 주석의 명세 내용(검증 순서, 처리 단계, 에러 코드)을 모두 반영했고, `NotImplementedYetException` 줄을 지웠습니다. 지갑·참여 상태를 바꾼다면 8장의 락 순서를 지켰고, 금액 변경은 `WalletLedger`, 인원·상태 변경 뒤에는 `potUpdated`, 명세에 알림이 적혀 있으면 `NotificationSender`를 호출했습니다. 서버가 validate를 통과해 뜨고, Swagger에서 성공 케이스와 대표 에러 케이스를 한 번씩 호출해 응답이 명세 예시와 같은 모양인지 확인했습니다.
