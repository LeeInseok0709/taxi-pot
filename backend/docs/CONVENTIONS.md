# 택시팟 백엔드 개발 규칙 (확정본)

기준: DB 테이블 설계서 13차 · API 명세서 3.9.0
틀(구조·공통 코드·시그니처)은 **기성이 확정**했다. 팀원은 정해진 자리에 **본문만** 채운다.
이 문서에 없는 결정이 필요하면 혼자 정하지 말고 기성에게 물어본다.

---

## 0. 한 줄 요약

> **내 담당 API의 `XxxService` 메서드 본문만 채운다. 컨트롤러·DTO·엔티티 컬럼·시그니처는 건드리지 않는다.**

---

## 1. 누가 무엇을 고치나

| 대상 | 수정 가능한 사람 | 팀원이 할 수 있는 것 |
|---|---|---|
| `build.gradle`, `application*.yml`, `global/**` 전체 | 기성 | 없음 (필요하면 기성에게 요청) |
| `global/code/*` enum 22개, `ErrorCode` 79개, `AppProperties` | 기성 | 없음 — 설계서·명세 값이 이미 전부 들어 있음 |
| `*/controller/*` (자동 생성 22개) | **아무도 안 고침** | 없음 — 명세와 1:1 |
| `*/dto/*` (자동 생성 68개) | **아무도 안 고침** | `static from(Entity)` 같은 정적 팩토리 메서드 추가만 |
| `*/entity/*` (자동 생성 14개) | 컬럼·타입·생성자는 **아무도 안 고침** | 그 도메인 담당이 맨 아래 '상태 변경 메서드' 영역에 메서드 추가 |
| `*/repository/*` | 그 도메인 담당 | 조회 메서드 추가 (기존 락 메서드 수정 금지) |
| `*/service/XxxService` (API 서비스 22개) | 표의 담당자 | **메서드 본문 구현**, private 메서드 추가 |
| `*/service/XxxManager` (공용 4개) | 그 도메인 담당 | 이미 있는 메서드 시그니처 변경 금지. 새 메서드 추가는 기성 확인 후 |
| `*/scheduler/*` | 표의 담당자 | 메서드 본문 구현 (이름·주기 변경 금지) |

담당 도메인: **기성** user·notification·global / **서연** pot(방·참여·입장 요청·통계)·report / **효우** wallet·settlement·rating / **인석** 도착 확인·스케줄러 1·2·3·4·11·chat

---

## 2. 계층과 주입 규칙 (어기면 서버가 안 뜬다)

```
Controller ──▶ XxxService(API 서비스) ──▶ XxxManager(공용) ──▶ Repository
                        │                         ▲
                        └──── 다른 도메인 Manager ──┘
```
1. **Controller**는 자기 `XxxService` 하나만 호출한다 (이미 생성됨).
2. **API 서비스**(`AuthService`, `PotService` ...)는 **어떤 도메인의 Manager든** 주입할 수 있다.
   **다른 API 서비스는 절대 주입하지 않는다.**
3. **Manager**는 아래 방향으로만 주입한다. 반대 방향 주입 금지.
   `NotificationManager` ← `UserManager` ← `WalletManager` ← `PotManager`
   (예: PotManager는 WalletManager를 써도 되지만, WalletManager는 PotManager를 쓰면 안 된다)
4. **Repository**: 자기 도메인은 자유. 다른 도메인 Repository는 **조회·락만**. 다른 도메인 테이블 **수정은 그 도메인 Manager**로.
5. **스케줄러**는 Manager·Repository를 쓴다. API 서비스 주입 금지.

> Spring Boot는 A↔B가 서로 주입하면 기동 자체를 거부한다 (`The dependencies of some of the beans form a cycle`).
> 위 규칙만 지키면 이 일은 구조적으로 생기지 않는다.

### 공용 Manager — 확정된 메서드 (이미 만들어져 있음)
| Manager | 메서드 | 상태 | 쓰는 곳 |
|---|---|---|---|
| `NotificationManager` | `send(memberId, potId, type, title, content)` | ✅ 구현됨 (푸시는 커밋 후, FCM 연결은 기성 TODO) | 전원 |
| `UserManager` | `getActiveUser(userId)` — 없음 404 / 탈퇴 403 | ✅ | 전원 |
| | `lockUser(userId)`, `lockUsers(ids)` — 락 1번 | ✅ | 서연·효우·인석 |
| | `validateCanUsePot(user)` — 학생 인증 → 정지 순서, detail 포함 | ✅ | 서연 |
| | `isSuspendedNow(user)` | ✅ | 서연 |
| | `suspend(userId, until)` — **모든 정지는 이것만**. 더 긴 정지 유지, null = 영구 | ✅ | 인석·서연·기성 |
| | `recordNoShow(userId)` — 노쇼 +1, 15분 정지 | ✅ | 인석 |
| `WalletManager` | `createWallet(memberId)` | ✅ | 기성(가입) |
| | `getWallet`, `lockWallet`, `lockWallets` — 락 2번 | ✅ | 전원 |
| | `calculateHoldAmount(expectedFare, maxMembers)` — floor(요금×1.2÷인원)+1,000 | ✅ | 서연 |
| | `hold(memberId, potId, amount)` — 처리 단계는 주석 | ⬜ 효우 구현 | 서연 |
| | `refundHold(memberId, potId)` — 처리 단계는 주석 | ⬜ 효우 구현 | 서연·인석 |
| `PotManager` | `getPot`, `lockPot` — 락 3번 | ✅ | 전원 |
| | `findActivePotId(userId)`, `hasActivePot(userId)` — 명세 '진행 중인 다른 방' | ✅ | 서연·기성(탈퇴) |

> **효우는 첫 주에 `hold`·`refundHold`부터 구현한다.** 서연의 방 생성·참여, 인석의 노쇼·자동 해산이 모두 이 두 메서드를 쓴다.

---

## 3. 락 순서 (명세 '동시성 제어')

**USERS → WALLET → TAXIPOT**, 여러 회원이면 id 오름차순. Manager의 `lockXxx` 메서드만 쓴다.
```java
// 방 참여 (PotMemberService.joinPot) — 방 정보가 먼저 필요해도 락은 이 순서
Taxipot snapshot = potManager.getPot(potId);              // 락 없이 읽기 (예치액 계산용)
User user = userManager.lockUser(userId);                  // 1
userManager.validateCanUsePot(user);
walletManager.lockWallet(userId);                          // 2
Taxipot pot = potManager.lockPot(potId);                   // 3 — 여기서부터 pot 값으로 다시 검증
int amount = walletManager.calculateHoldAmount(pot.getExpectedFare(), pot.getMaxMembers());
walletManager.hold(userId, potId, amount);
```
방장이 다른 회원을 처리할 때(내보내기·입장 요청 수락)는 **대상 회원**의 USERS → WALLET → TAXIPOT 순서.

---

## 4. 서비스 구현 규칙

- 실패는 `throw new BusinessException(ErrorCode.XXX)` 만. detail이 필요한 16개 코드(명세 'ErrorResponse.detail 규격')는
  `new BusinessException(ErrorCode.POT_FULL, ErrorDetail.of("maxMembers", 4, "currentMembers", 4))`
  (`Map.of()` 금지 — null 값에서 터짐. 일시는 LocalDateTime을 넣어도 자동으로 +09:00 변환)
- 값이 들어가는 문구는 message 인자로: `"지갑 잔액이 %,d원 부족합니다.".formatted(shortage)`
- 현재 시각은 `TimeUtil.now()`만. Entity → DTO 일시 변환은 `TimeUtil.toOffset(...)`, DTO → Entity는 `TimeUtil.toKst(...)`
- 15분·50m 같은 숫자는 `AppProperties`에서. 코드에 숫자 직접 쓰기 금지
- 서비스는 `Long userId` 파라미터만 사용. SecurityContext 직접 접근 금지 (이미 시그니처에 들어 있음)
- `BusinessException`을 catch해서 삼키지 않는다 (트랜잭션이 rollback-only가 되어 커밋 때 500)
- Entity를 반환하지 않는다. DTO 변환은 서비스 안에서 (`open-in-view: false`)
- Entity → DTO 변환: DECIMAL(위도·경도·평점) `BigDecimal` → DTO `Double`은 `value.doubleValue()`
- 파일 업로드: `fileStorage.save(file, "captures")` / `"receipts"`, 조회: `return fileStorage.loadImage(path);`
- 페이지 응답: `return PageResponse.of(page, XxxResponse::from);` (서비스는 이미 `Pageable pageable`을 받음)
- 스케줄러: 스케줄러 클래스에는 `@Transactional`을 붙이지 않고, 방·회원 하나씩 **다른 Bean**의 `@Transactional` 메서드를 호출한다

### Entity 상태 변경 메서드 작성법
```java
// ===== 상태 변경 메서드 (pot 담당이 추가) =====
public void markArrived(LocalDateTime now) {
    this.arrivedAt = now;
}
```
Setter 금지. 이름은 동작 그대로 (`close`, `cancel`, `markArrived`, `rejoin`).
`TaxipotMember.joinedAt`, `Withdrawal`처럼 생성 시각을 직접 넣는 컬럼은 생성자에 `TimeUtil.now()`를 넘긴다.

---

## 5. 개발 중 테스트

- 서버 실행 → http://localhost:8080/swagger-ui.html 에 API 76개가 이미 다 보인다
- JWT 완성 전: 각 API의 **X-Debug-User-Id** 칸에 회원 id 입력 (application-local.yml의 `debug-header-enabled: true`)
- 아직 구현 안 된 API를 호출하면 500 `"아직 구현되지 않은 API입니다: TODO(담당) ..."` — 정상
- 같은 DB를 여러 명이 공유할 때는 `app.scheduler.enabled: false` (서로의 방을 자동 퇴장시키는 사고 방지)

---

## 6. Git

- `main`(시연, 직접 push 금지) ← `develop`(통합) ← `feature/{도메인}-{기능}` (예: `feature/pot-join`)
- PR은 develop으로, 1명 이상 승인 후 merge. GitHub에서 main·develop 브랜치 보호 설정
- PR 단위: API 1~3개. 하루 한 번 이상 develop을 내 브랜치에 merge
- 커밋: `feat(pot): 방 참여 API` / `fix(wallet): ...` / `refactor` / `docs` / `chore`
- 커밋 전 확인: `application-local.yml`, `uploads/`, `.idea/` 제외됐는지
- IntelliJ: Settings → Editor → Code Style → **Enable EditorConfig support** 켜기

> 위 1장 규칙대로면 두 사람이 같은 파일을 고치는 경우는 거의 없다 (서비스 파일이 담당자별로 나뉘어 있음).
> 예외: `pot/entity/TaxipotMember` 상태 변경 메서드는 서연·인석이 같이 추가할 수 있다 → 추가 전에 카톡으로 메서드 이름을 먼저 공유.

---

## 7. 기성이 할 일 (틀 담당 TODO)

1. JWT: `SecurityConfig`의 TODO 두 곳 + 토큰 버전 검사 필터 (principal = Long 회원 id)
2. FCM: `FcmPushListener`
3. 명세서에 반영: 에러 코드 `INTERNAL_SERVER_ERROR`(500)·`METHOD_NOT_ALLOWED`(405) 추가, 지각 경고 N분(현재 2분 가정) 확정, `GET /api/health` 추가
4. 앱(지연·유나)에 전달: `TOKEN_REVOKED`는 재발급하지 말고 로그인 화면으로, 모든 enum에 UNKNOWN

---

## 8. API 담당표 (전체)

| 담당 | API | 구현할 곳 (XxxController.method → XxxService.method) |
|---|---|---|
| 기성 | `POST /api/auth/signup` | AuthController.signup |
| 기성 | `GET /api/auth/nickname-check` | AuthController.checkNicknameAvailability |
| 기성 | `POST /api/auth/login` | AuthController.login |
| 기성 | `POST /api/auth/refresh` | AuthController.refresh |
| 기성 | `POST /api/auth/logout` | AuthController.logout |
| 기성 | `GET /api/universities` | ReferenceController.getUniversityList |
| 기성 | `GET /api/banks` | ReferenceController.getBankList |
| 기성 | `POST /api/verifications/email-code` | VerificationController.sendEmailCode |
| 기성 | `POST /api/verifications/email-code/confirm` | VerificationController.confirmEmailCode |
| 기성 | `POST /api/verifications/capture` | VerificationController.submitCapture |
| 기성 | `GET /api/verifications/me` | VerificationController.getMyVerification |
| 기성 | `GET /api/users/me` | UserController.getMyProfile |
| 기성 | `PATCH /api/users/me` | UserController.updateMyProfile |
| 기성 | `POST /api/users/me/withdraw` | UserController.withdraw |
| 기성 | `PATCH /api/users/me/password` | UserController.changePassword |
| 기성 | `GET /api/users/me/account` | UserController.getMyAccount |
| 기성 | `PUT /api/users/me/account` | UserController.updateMyAccount |
| 기성 | `PUT /api/users/me/fcm-token` | UserController.updateFcmToken |
| 기성 | `GET /api/users/{userId}` | UserController.getUserProfile |
| 기성 | `GET /api/notifications` | NotificationController.getNotificationPage |
| 기성 | `GET /api/notifications/unread-count` | NotificationController.getUnreadCount |
| 기성 | `PATCH /api/notifications/{notificationId}/read` | NotificationController.markAsRead |
| 기성 | `PATCH /api/notifications/read-all` | NotificationController.markAllAsRead |
| 기성 | `GET /api/admin/verifications` | AdminVerificationController.getVerificationPage |
| 기성 | `GET /api/admin/verifications/{verificationId}` | AdminVerificationController.getVerification |
| 기성 | `PATCH /api/admin/verifications/{verificationId}` | AdminVerificationController.reviewVerification |
| 기성 | `GET /api/admin/verifications/{verificationId}/capture` | AdminVerificationController.getCaptureImage |
| 기성 | `GET /api/admin/users` | AdminUserController.getUserPage |
| 기성 | `GET /api/admin/users/{userId}` | AdminUserController.getUserDetail |
| 기성 | `PATCH /api/admin/users/{userId}/status` | AdminUserController.updateUserStatus |
| 서연 | `GET /api/users/me/pots` | MyPotController.getMyPotHistoryPage |
| 서연 | `GET /api/users/me/reports` | ReportController.getMyReportPage |
| 서연 | `POST /api/reports` | ReportController.createReport |
| 서연 | `GET /api/pots` | PotController.getPotPage |
| 서연 | `POST /api/pots` | PotController.createPot |
| 서연 | `GET /api/pots/{potId}` | PotController.getPotDetail |
| 서연 | `PATCH /api/pots/{potId}` | PotController.updatePot |
| 서연 | `DELETE /api/pots/{potId}` | PotController.cancelPot |
| 서연 | `POST /api/pots/{potId}/start` | PotController.startPot |
| 서연 | `POST /api/pots/{potId}/members` | PotMemberController.joinPot |
| 서연 | `DELETE /api/pots/{potId}/members/me` | PotMemberController.leavePot |
| 서연 | `PATCH /api/pots/{potId}/members/me/seat` | PotMemberController.changeSeat |
| 서연 | `POST /api/pots/{potId}/members/{userId}/kick` | PotMemberController.kickMember |
| 서연 | `POST /api/pots/{potId}/join-requests` | JoinRequestController.createJoinRequest |
| 서연 | `GET /api/pots/{potId}/join-requests` | JoinRequestController.getJoinRequestList |
| 서연 | `DELETE /api/pots/{potId}/join-requests/me` | JoinRequestController.cancelJoinRequest |
| 서연 | `POST /api/pots/{potId}/join-requests/{userId}/accept` | JoinRequestController.acceptJoinRequest |
| 서연 | `POST /api/pots/{potId}/join-requests/{userId}/reject` | JoinRequestController.rejectJoinRequest |
| 서연 | `GET /api/admin/users/{userId}/pots` | AdminPotController.getUserPotHistoryPage |
| 서연 | `GET /api/admin/pots` | AdminPotController.getPotPage |
| 서연 | `GET /api/admin/statistics` | AdminPotController.getStatistics |
| 서연 | `GET /api/admin/reports` | AdminReportController.getReportPage |
| 서연 | `PATCH /api/admin/reports/{reportId}` | AdminReportController.processReport |
| 효우 | `GET /api/wallet` | WalletController.getMyWallet |
| 효우 | `GET /api/wallet/history` | WalletController.getPointHistoryPage |
| 효우 | `GET /api/wallet/debts` | WalletController.getDebtList |
| 효우 | `POST /api/wallet/debts/{settlementId}/pay` | WalletController.payDebt |
| 효우 | `POST /api/wallet/withdrawals` | WalletController.createWithdrawal |
| 효우 | `GET /api/wallet/withdrawals` | WalletController.getWithdrawalPage |
| 효우 | `POST /api/payments` | PaymentController.createPayment |
| 효우 | `GET /api/payments` | PaymentController.getPaymentPage |
| 효우 | `POST /api/payments/confirm` | PaymentController.confirmPayment |
| 효우 | `POST /api/pots/{potId}/fare-claim` | FareClaimController.createFareClaim |
| 효우 | `GET /api/pots/{potId}/settlement` | FareClaimController.getPotSettlement |
| 효우 | `GET /api/pots/{potId}/receipt` | FareClaimController.getReceiptImage |
| 효우 | `GET /api/pots/{potId}/ratings/targets` | RatingController.getRatingTargetList |
| 효우 | `POST /api/pots/{potId}/ratings` | RatingController.createRatings |
| 효우 | `PATCH /api/admin/users/{userId}/wallet` | AdminWalletController.adjustWallet |
| 효우 | `GET /api/admin/fare-claims` | AdminFareClaimController.getFareClaimPage |
| 효우 | `PATCH /api/admin/pots/{potId}/fare-claim` | AdminFareClaimController.reviewFareClaim |
| 효우 | `GET /api/admin/pots/{potId}/receipt` | AdminFareClaimController.getReceiptImage |
| 효우 | `POST /api/admin/pots/batch-status` | AdminFareClaimController.updateBatchStatus |
| 인석 | `POST /api/pots/{potId}/members/me/arrival` | ArrivalController.confirmArrival |
| 인석 | `GET /api/pots/{potId}/messages` | ChatController.getMessages |
| 인석 | `POST /api/pots/{potId}/messages` | ChatController.sendMessage |
| 인석 | `GET /api/quick-blocks` | ChatController.getQuickBlockList |

### 스케줄러 담당
| 번호 | 클래스.메서드 | 담당 |
|---|---|---|
| 1·2·3·4·11 | `pot/scheduler/PotScheduler` | 인석 |
| 5·6 | `user/scheduler/UserScheduler` | 기성 |
| 7 | `rating/scheduler/RatingScheduler` | 효우 |
| 8 | `wallet/scheduler/DebtScheduler` | 효우 |
| 9·10 | `settlement/scheduler/SettlementScheduler` | 효우 |

인석은 API 수가 적은 대신 스케줄러 5개와 채팅 STOMP 설정(`/ws`, `/topic/pots/{potId}`)을 맡는다.
