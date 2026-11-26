# 신고·별점·운영자 회원 관리·통계 구현 정리

`is/report-rating-admin` 브랜치에서 구현한 내용입니다. 기준은 `is/member-rating-jobs`(PR #17, 스케줄러 5·6·7) 위입니다. 별점 마감 계산이 PR #17의 쿼리와 같은 기준을 써야 해서 그 위에 쌓았습니다. **PR #17을 먼저 머지한 뒤 이 PR을 머지**해 주세요. 담당은 `CONVENTIONS.md` 1장 회원·운영 칸의 '운영(신고·별점·운영자 회원 관리·통계)' 부분입니다.

## 1. 바뀐 파일

경로는 모두 `src/main/java/com/taxipot/domain/` 아래입니다.

| 파일 | 구분 | 내용 |
|---|---|---|
| `report/service/ReportService.java` | 수정 | 신고 접수, 내 신고 내역 |
| `report/service/AdminReportService.java` | 수정 | 운영자 신고 목록, 신고 처리(승인·반려) |
| `report/service/ReportResponseMapper.java` | 새 파일 | 신고 → 응답 변환 (닉네임·방 제목을 한 번에 조회) |
| `report/repository/ReportRepository.java` | 수정 | 선택 필터 검색, 처리용 잠금 조회 |
| `rating/service/RatingService.java` | 수정 | 평가 대상 조회, 평가 제출 |
| `rating/repository/RatingRepository.java` | 수정 | 방의 정산 완료 시각 조회 (`findSettledAt`) |
| `user/service/AdminUserService.java` | 수정 | 회원 목록(검색), 정지·해제 |
| `user/service/AdminUserDetailFacade.java` | 새 파일 | 회원 상세 (지갑·인증·신고·이용 기록 모음) |
| `user/dto/AdminUserDetailResponse.java` | 수정 | 빈 record → 명세 필드 채움 (아래 4장) |
| `user/controller/AdminUserController.java` | 수정 | 회원 상세만 Facade 호출로 변경 |
| `user/repository/UserRepository.java` | 수정 | 운영자 회원 검색 쿼리 |
| `admin/service/StatisticsService.java` | 수정 | 매칭 통계 |
| `admin/repository/StatisticsRepository.java` | 새 파일 | 통계 집계 쿼리 전용 |
| `backend/tools/gen_api.py` | 수정 | 회원 상세 매핑을 Facade 로 (재생성해도 같게) |

다른 사람 브랜치(`gg/user-profile`, `hw/*`, `sy/chat`)와 겹치는 파일은 없습니다. 엔티티는 수정하지 않았고, 미리 만들어 둔 메서드(`Report.approve/reject`, `User.increaseReportCount/suspend/releaseSuspension/changeRating`, `Rating.submit`)만 썼습니다.

## 2. 신고

### 신고 접수 `POST /api/reports`
| 순서 | 검사 | 에러 |
|---|---|---|
| 1 | 본인 신고 | 400 `SELF_REPORT_NOT_ALLOWED` |
| 2 | 자격: ① 둘 다 그 방 BOARDED, 또는 ② 피신고자 NO_SHOW + 신고자 JOINED·BOARDED | 403 `REPORT_NOT_ELIGIBLE` |
| 3 | ②로만 자격이 되는데 type ≠ NO_SHOW | 400 `INVALID_INPUT` |
| 4 | 같은 방·같은 대상 중복 (동시 클릭으로 UNIQUE 위반이 나도 같은 에러) | 409 `DUPLICATE_REPORT` |

접수만으로는 `report_count`가 바뀌지 않습니다. 운영자가 승인할 때 오릅니다.

### 신고 처리 `PATCH /api/admin/reports/{reportId}`
- **잠금**: 피신고자 USERS → REPORT 순서로 잠근 뒤 PENDING인지 다시 확인합니다. 운영자 두 명이 같은 신고를 동시에 승인해도 횟수가 한 번만 오릅니다.
- **승인**: `report_count + 1`, `clean_streak = 0`. 누적이 **3회가 되면 3일**, **6회가 되면 7일**, **10회 이상이면 영구 정지**입니다. 이미 더 긴 정지 중이면 `User.suspend`가 기존 값을 유지합니다. 신고 유형이 NO_SHOW여도 `no_show_count`는 바꾸지 않습니다.
- **반려**: 피신고자 변경 없음.
- **알림**: 승인·반려 모두 신고자와 피신고자에게 `REPORT_RESULT`를 보냅니다.

## 3. 별점

### 평가 마감 = 스케줄러 7과 같은 시계
평가 기간은 **그 방 SETTLEMENT `settled_at`의 최댓값(운영자 청구 승인 시각) + 24시간 '전'까지**입니다. `RatingRepository.findSettledAt`으로 읽고, 스케줄러 7(`findPotIdsMissingRatings`)도 같은 값을 씁니다.

```
정산 완료 ───────── 24시간 ─────────┤ 마감
  [ 회원 제출 가능 (API) ]           [ 정각부터: API 409 · 스케줄러 7이 ★3 기본값 ]
```

API는 `now < 마감`일 때만 받고, 스케줄러는 `마감 ≤ now`인 방만 채우므로 틈이나 겹침이 없습니다. 그래도 같은 순간에 겹쳐 UNIQUE 위반이 나면 409 `ALREADY_RATED`로 바꿉니다.

### 평가 제출 `POST /api/pots/{potId}/ratings`
| 순서 | 검사 | 에러 |
|---|---|---|
| 1 | 내가 그 방 BOARDED | 403 `NOT_POT_MEMBER` |
| 2 | 방 COMPLETED이고 정산 완료 시각이 있음 | 409 `INVALID_POT_STATUS` |
| 3 | 마감 전 | 409 `RATING_PERIOD_EXPIRED` |
| 4 | 항목마다: 본인 아님 / 한 요청에 같은 대상 중복 없음 / 대상도 BOARDED / 평균 3 미만이면 사유 필수 / 이미 평가 안 함 | 400 `SELF_RATING_NOT_ALLOWED`·`INVALID_INPUT` / 403 / 400 `BAD_REASON_REQUIRED` / 409 `ALREADY_RATED` |

전부 통과하면 피평가자 USERS를 id 오름차순으로 잠그고 저장합니다. 평균 4 이상이면 +0.2, 3 미만이면 −0.2입니다. 요청 전체가 한 트랜잭션이라 하나라도 실패하면 아무것도 저장되지 않습니다.

평가 대상 조회는 나를 뺀 BOARDED 탑승자와 내가 이미 평가했는지(기본 평가 포함)를 돌려줍니다.

## 4. 운영자 회원 관리

- **목록** `GET /api/admin/users`: 이름·닉네임·이메일·학번 부분 일치 검색과 상태 필터가 있고, 최근 가입순입니다. 전화번호는 암호화 컬럼이라 검색하지 않습니다. 입력에 섞인 `%`·`_`는 글자 그대로 찾습니다.
- **정지·해제** `PATCH .../status`: 회원 행을 잠그고 처리합니다. SUSPENDED + 시각 없음이면 영구 정지, 과거 시각이면 400입니다. ACTIVE면 해제(영구 정지도 여기서만 풀림), 탈퇴 회원이면 403 `WITHDRAWN_USER`입니다.
- **상세** `GET /api/admin/users/{userId}`: 지갑 잔액·예치금·미수금, 학생 인증 이력, 받은 신고(최근 100건), 최근 이용 기록 10건을 모읍니다.
  - 회원(user) 서비스는 notification만 부를 수 있어서(`CONVENTIONS.md` 5장), 신고·이용 기록 서비스까지 부를 수 있는 **`AdminUserDetailFacade`**로 옮겼습니다. 컨트롤러는 이 호출 한 줄만 바뀌었고, `gen_api.py` 매핑도 맞춰 두었습니다.
  - `AdminUserDetailResponse`는 명세가 `allOf` 구조라 `gen_dto.py`가 빈 record를 만들었습니다. 명세 순서대로 손으로 채우고 파일 주석에 표시했습니다.
  - 인증 이력 변환은 기성님 `AdminVerificationService.toResponse`(아직 `gg/user-profile`에만 있음)와 같은 모양으로 맞췄습니다. 그쪽이 머지되면 공용 변환기로 합칠 수 있습니다.

## 5. 통계 `GET /api/admin/statistics`

- **기간**: 출발일 기준이고 from·to 모두 포함입니다. 둘 다 없으면 오늘 포함 최근 30일, 하나만 있으면 30일 폭으로 채웁니다. from > to면 400입니다.
- **limit**: 1~50, 기본 10.
- **totalPots**는 CANCELED를 포함한 전체이고, `completedPots`·`canceledPots`는 상태별 개수입니다. 출발지·목적지 순위와 시간대별 개수는 **CANCELED 제외**입니다.
- **시간대**는 0~23시 24칸을 모두 채웁니다(방이 없으면 0). 그래프를 바로 그릴 수 있게 하려는 것입니다.
- 쿼리는 `StatisticsRepository`에 따로 두어 택시팟 담당의 `TaxipotRepository`를 건드리지 않았습니다.

## 6. 팀 확인 필요

1. **신고 누적 정지 경계**: 명세의 '3회·6회·10회'를 '그 횟수에 도달한 순간'으로 해석했습니다. 클린 탑승으로 횟수가 줄었다가 다시 3이 되면 다시 3일 정지됩니다.
2. **totalPots에 CANCELED 포함 여부**: 명세 문장은 'CANCELED 제외'인데 응답에 `canceledPots`가 따로 있어서, total은 전체·순위는 제외로 정했습니다.
3. **알림 문구**: 신고 결과 알림 문구는 명세에 없어 임의로 정했습니다.
4. **받은 신고 개수**: 상세 화면의 받은 신고는 최근 100건까지만 보여 줍니다(명세에 제한 없음).
5. **평가 대상 조회의 방 상태**: 명세는 'COMPLETED 방에서'인데, 상태는 따로 검사하지 않고 BOARDED 여부만 봅니다. 탑승 시작 후 아직 정산 전이면 목록은 나오고, 제출은 409가 됩니다.

## 7. 확인 방법

`backend`에서 `.\gradlew.bat compileJava`로 컴파일을 확인합니다. 새 JPQL 쿼리(신고 검색, 회원 검색, 정산 시각, 통계 4개)는 서버를 띄울 때 검사되므로 서버를 한 번 실행해 에러 없이 뜨는지 봅니다.
