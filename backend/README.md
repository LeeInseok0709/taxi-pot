# taxipot-server

택시팟 백엔드 (Spring Boot 3.3 · Java 17 · MySQL 8.0)
기준 문서: DB 테이블 설계서 13차, API 명세서 3.9.0
**개발 규칙: [docs/CONVENTIONS.md](docs/CONVENTIONS.md) — 코드 짜기 전에 반드시 읽기 (8장에 내 담당 API 표)**

## 들어 있는 것
| 위치 | 내용 |
|---|---|
| `db/schema.sql` | 설계서 13차 CREATE문 14개 (ALTER문 제외) |
| `global/` | 공통 에러 응답, ErrorCode 79개, enum 22개, 시간(KST)·설정값·암호화·파일 업로드·페이징·인증 리졸버·미수금 인터셉터 |
| `*/entity` | 엔티티 14개 (설계서와 1:1) |
| `*/repository` | 리포지토리 14개 + 락 메서드 |
| `*/controller`, `*/dto` | 명세 API 76개, DTO 68개 (자동 생성, 수정 금지) |
| `*/service/XxxService` | API별 서비스 메서드 (본문 TODO → 담당자가 구현) |
| `*/service/XxxManager` | 도메인 공용 기능 (정지·락·예치액·알림 등) |
| `*/scheduler` | 스케줄러 11개 자리 |

## 처음 1회 (기성 — 레포 올리기 전)
1. IntelliJ에서 이 폴더를 열고 `build.gradle`을 Gradle 프로젝트로 불러온다.
2. Gradle 창 → Tasks → build setup → `wrapper` 실행 → `gradlew`, `gradle/wrapper/` 생성. 이것까지 커밋.
3. 아래 '팀원 실행 방법'대로 실행해 `/api/health` 확인 → `main` push → `develop` 브랜치 생성 → 브랜치 보호 설정.

## 팀원 실행 방법
1. MySQL 8.0에 DB 생성
   ```sql
   CREATE DATABASE taxipot CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
2. 테이블 생성
   ```
   mysql -u root -p --default-character-set=utf8mb4 taxipot < db/schema.sql
   ```
   Workbench로 실행해도 되지만 연결 문자셋이 utf8mb4여야 한다 (아니면 '탈퇴회원_' CHECK가 깨짐).
3. `src/main/resources/application-local.yml.example`을 복사해 `application-local.yml`로 이름 변경 → DB 비밀번호, 팀 공용 JWT 시크릿·AES 키(기성이 공유) 입력
4. `TaxipotApplication` 실행
5. 확인
   - http://localhost:8080/api/health → `"serverTime": "...+09:00"`
   - http://localhost:8080/swagger-ui.html → API 76개 표시, 각 API에 X-Debug-User-Id 칸

## 서버가 안 뜰 때
| 로그 | 원인 |
|---|---|
| `Schema-validation: missing table [...]` | schema.sql을 안 돌렸거나 DB 이름이 다름 |
| `Schema-validation: wrong column type ...` | 엔티티 컬럼을 손댐 — 원래대로 되돌릴 것 |
| `The dependencies of some of the beans form a cycle` | API 서비스끼리 주입했거나 Manager를 거꾸로 주입함 (CONVENTIONS 2장) |
| `app.crypto.aes-key는 Base64로...` | application-local.yml에 공용 AES 키 미입력 |
| `Access denied for user` | application-local.yml의 DB 계정·비밀번호 |

## 리눅스 서버에 올릴 때
MySQL 설치 직후(데이터 넣기 전) `lower_case_table_names=1` 설정. 설계서 테이블명은 대문자(USERS)이고
JPA는 소문자(users)로 찾기 때문에 리눅스 기본값(대소문자 구분)이면 테이블을 못 찾는다. 윈도우·맥은 상관없음.
