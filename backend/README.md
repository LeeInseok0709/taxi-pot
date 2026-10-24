# 택시팟 서버 (Spring Boot)

API 명세서 3.9.0과 DB 설계서 13차를 기준으로 만든 백엔드 기본 틀입니다. 공통 기반(인증·에러·일시·업로드·암호화·락·원장·알림·실시간)과 회원가입·로그인·이메일 인증·알림 API는 구현과 동작 확인을 마쳤습니다. 나머지 API는 경로·요청·응답 형식이 명세대로 만들어진 상태이고, 담당자가 서비스 메서드 안을 채우면 됩니다. 아직 채우지 않은 API를 호출하면 `501 NOT_IMPLEMENTED`가 돌아옵니다.

팀 규칙은 [docs/CONVENTIONS.md](docs/CONVENTIONS.md)에 있습니다. 코드를 쓰기 전에 반드시 읽어 주세요.

## 개발 환경

JDK 17 이상(17·21 모두 가능, 결과물은 Java 17 호환), MySQL 8.0.16 이상, IntelliJ IDEA를 씁니다. Gradle은 래퍼(`gradlew`)가 들어 있어 따로 설치하지 않아도 됩니다. Spring Boot 3.5.15, Hibernate 6.6, jjwt 0.12, springdoc 2.8 기준입니다.

## 처음 실행하기

1. **DB와 계정 만들기** (MySQL root로 한 번)
   ```bash
   mysql -u root -p < db/create-database.sql
   ```
2. **테이블 만들기**: `db/schema.sql`은 설계서 DDL 시트에서 CREATE 문만 뽑은 파일입니다. 시트 아래쪽의 ALTER 문은 예전 DB를 옮길 때 쓰는 것이라 넣지 않았습니다.
   ```bash
   mysql -u root -p --default-character-set=utf8mb4 taxipot < db/schema.sql
   ```
   `--default-character-set=utf8mb4`를 빼면 한글 COMMENT와 '탈퇴회원_' CHECK가 깨질 수 있습니다. 실제 학교로 바꾸려면 `UNIVERSITY` 테이블을 수정하세요(예시로 '한국대학교 / university.ac.kr' 1건이 들어 있습니다).
3. **IntelliJ로 열기**: `File > Open`에서 이 폴더를 고르고 Gradle 프로젝트로 엽니다. `Settings > Build, Execution, Deployment > Build Tools > Gradle`에서 Gradle JVM을 17 이상으로 맞추고, `Settings > Build > Compiler > Annotation Processors`에서 `Enable annotation processing`을 켭니다(Lombok).
4. **실행**: `TaxipotApplication`을 실행하면 기본 프로필 `local`로 뜹니다. 로그에 `Started TaxipotApplication`이 나오고, 처음 실행이면 `운영자 계정 생성: admin@taxipot.local`도 나옵니다.

서버가 뜨지 않고 `Schema-validation: wrong column type ...` 또는 `missing table ...`이 나오면 Entity와 DB가 어긋난 것입니다. 일부러 `ddl-auto: validate`로 둬서 바로 알 수 있게 했습니다. 대부분은 2단계를 빠뜨렸거나 예전 스키마가 남아 있는 경우이니 `db/reset-local.sql`로 DB를 비우고 2단계부터 다시 하세요.

## 테스트하기 (Swagger)

`http://localhost:8080/swagger-ui.html`을 엽니다. 오른쪽 위 목록에서 두 문서를 고를 수 있습니다. '택시팟 API 명세서 3.9.0 (공식)'은 팀이 합의한 원본이고, '서버 구현 (자동)'은 지금 서버 코드에서 만든 문서라 여기서 실제로 호출해 볼 수 있습니다.

로그인이 필요한 API는 두 가지 방법으로 부릅니다. 첫째, `POST /api/auth/login` 응답의 `accessToken`을 Swagger의 `Authorize`에 넣습니다. 둘째, local 프로필에서만 각 API의 `X-Debug-User-Id` 칸에 회원 번호를 넣으면 토큰 없이 그 회원으로 호출됩니다. 운영자는 `admin@taxipot.local / admin1234!`입니다.

local 프로필은 메일을 보내지 않고 인증코드를 서버 로그에 찍습니다(`[인증코드-로그 발송] 이메일 → 123456`). 가입 직후 1분 안에 그 코드를 `POST /api/verifications/email-code/confirm`에 넣으면 됩니다.

공통 기반이 제대로 동작하는지는 서버를 띄운 상태에서 아래 스크립트로 한 번에 확인할 수 있습니다(41개 항목).
```bash
python tools/smoke_test.py http://localhost:8080 <서버 로그 파일 경로>   # Mac·리눅스는 python3
```

## 프로필과 설정값

`local`은 개발용이고 Debug 헤더가 켜져 있으며 스케줄러는 꺼져 있습니다. `demo`는 시연용으로 스케줄러를 켜고 15분·30분·3일 같은 시간 값을 몇 분 단위로 줄였습니다(`--spring.profiles.active=demo`). 명세에 '서버 설정값'으로 적힌 숫자는 모두 `application.yml`의 `app.*`에 있으니 코드에 숫자를 직접 쓰지 마세요.

운영·시연 서버에서는 환경변수로 비밀값을 넣습니다. `TAXIPOT_JWT_SECRET`(32바이트 이상 문자열), `TAXIPOT_CRYPTO_KEY`(Base64로 인코딩한 32바이트, `openssl rand -base64 32`), `DB_HOST`·`DB_USERNAME`·`DB_PASSWORD`, 메일을 실제로 보낼 때는 `app.mail.enabled=true`와 `MAIL_HOST`·`MAIL_USERNAME`·`MAIL_PASSWORD`가 필요합니다. 암호화 키를 바꾸면 이미 저장된 휴대폰·계좌번호를 읽을 수 없게 되므로, 한번 정한 키는 바꾸지 마세요.

## 폴더 구조

```
db/                 schema.sql(CREATE만), create-database.sql, reset-local.sql
docs/               CONVENTIONS.md(팀 규칙), FRONTEND_NOTES.md(앱·웹 전달 사항)
tools/              gen_dto.py·gen_api.py(명세 → DTO·컨트롤러 생성기), smoke_test.py
src/main/java/com/taxipot/
  global/           공통 기반 (담당: 공통·인증). 도메인 코드를 import 하지 않는다
    code/           enum 22개 (DB '코드 정의'와 1:1)
    config/ error/ security/ time/ web/ file/ crypto/ response/ util/ entity/
  domain/<도메인>/  controller · service · entity · repository · dto
  domain/scheduler/ 스케줄러 11개 작업의 실행 시각 (로직은 각 *JobService)
src/main/resources/
  application*.yml
  static/api-spec/taxipot-api.yaml   공식 명세 (Swagger 와 ErrorCodeSpecTest 가 읽음)
```

## 검증 상태

이 틀은 Spring Boot 3.5.15·Hibernate 6.6 라이브러리로 전체 컴파일했고, MySQL 8.0.46에 `db/schema.sql`을 적용한 뒤 `ddl-auto: validate`로 서버가 정상 기동하는 것(14개 테이블 매핑 일치)을 확인했습니다. 스모크 테스트 41개, 미수금 이용 제한·토큰 무효화·암호화 저장 확인, `ErrorCodeSpecTest`(명세 에러 코드 77개와 이름·HTTP 상태 일치)도 통과했습니다. 단, 작성 환경 사정으로 JDBC 드라이버는 MySQL Connector/J 대신 MariaDB 드라이버로 시험했고 Gradle 빌드 자체는 돌려 보지 못했습니다. 처음 `./gradlew build`나 IntelliJ 실행에서 오류가 나면 로그 전체를 공유해 주세요. STOMP(웹소켓)는 컴파일과 기동까지만 확인했고 실제 앱 연결은 시험하지 않았습니다.
