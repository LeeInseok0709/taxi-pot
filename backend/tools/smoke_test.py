"""공통 기반 동작 확인용 스모크 테스트 (서버를 local 프로필로 띄운 뒤 실행).
python3 tools/smoke_test.py [http://localhost:8080] [서버로그경로]
- 서버 로그에서 이메일 인증코드를 읽으므로 app.mail.enabled=false 여야 한다.
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080'
LOG = sys.argv[2] if len(sys.argv) > 2 else None
results = []


def call(method, path, body=None, token=None, headers=None, raw=None, ctype='application/json'):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        req.add_header('Content-Type', ctype)
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            text = r.read().decode()
            return r.status, (json.loads(text) if text and 'json' in r.headers.get('Content-Type', '') else text)
    except urllib.error.HTTPError as e:
        text = e.read().decode()
        try:
            return e.code, json.loads(text)
        except ValueError:
            return e.code, text


def check(name, cond, info=''):
    results.append((name, bool(cond)))
    print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else f'  → {info}'))


s, b = call('GET', '/api/universities')
check('학교 목록(공개 API)', s == 200 and b and b[0]['name'] == '한국대학교', b)
s, b = call('GET', '/api/banks')
check('은행 목록 이름순', s == 200 and [x['name'] for x in b] == sorted(x['name'] for x in b), b)
s, b = call('GET', '/api/auth/nickname-check?nickname=' + urllib.request.quote('탈퇴회원_1'))
check('예약 닉네임 → RESERVED', s == 200 and b == {'available': False, 'reason': 'RESERVED'}, b)

tag = uuid.uuid4().hex[:6]
signup = {'email': f'Hong{tag}@University.ac.kr', 'password': 'Password123!', 'name': '홍길동', 'nickname': f'길동{tag}',
          'phone': '010-1234-5678', 'gender': 'M', 'university': '한국대학교', 'studentNo': f'2023{tag}',
          'bankCode': '004', 'accountNumber': '123-456-789012', 'accountHolder': '홍길동', 'agreeRequired': True}
s, b = call('POST', '/api/auth/signup', dict(signup, email=f'x{tag}@gmail.com'))
check('이메일 도메인 불일치 → 400 EMAIL_DOMAIN_MISMATCH', s == 400 and b['code'] == 'EMAIL_DOMAIN_MISMATCH', b)
s, b = call('POST', '/api/auth/signup', dict(signup, agreeRequired=False))
check('약관 미동의 → TERMS_NOT_AGREED', s == 400 and b['code'] == 'TERMS_NOT_AGREED', b)
s, b = call('POST', '/api/auth/signup', dict(signup, phone='12345'))
check('형식 오류 → INVALID_INPUT + detail null 키 존재', s == 400 and b['code'] == 'INVALID_INPUT' and 'detail' in b and b['detail'] is None, b)
s, b = call('POST', '/api/auth/signup', signup)
check('회원가입 200 + 토큰', s == 200 and b.get('accessToken') and b['tokenType'] == 'Bearer', b)
token, refresh, user = b['accessToken'], b['refreshToken'], b['user']
check('응답 isVerified 키 유지(is 제거 안 됨)', 'isVerified' in user and user['isVerified'] is False, user)
check('null 필드도 키 유지(suspendedUntil)', 'suspendedUntil' in user and user['suspendedUntil'] is None, user)
check('일시 +09:00 형식', re.match(r'^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\+09:00$', user['createdAt']), user['createdAt'])
check('이메일 소문자 정규화·휴대폰 숫자만', user['email'] == signup['email'].lower() and user['phone'] == '01012345678', user)
s, b = call('POST', '/api/auth/signup', dict(signup, email=f'other{tag}@university.ac.kr'))
check('중복 닉네임 → 409 DUPLICATE_NICKNAME', s == 409 and b['code'] == 'DUPLICATE_NICKNAME', b)

s, b = call('GET', '/api/users/me')
check('토큰 없음 → 401 UNAUTHORIZED(JSON)', s == 401 and b['code'] == 'UNAUTHORIZED', b)
s, b = call('GET', '/api/users/me', token=token)
check('내 정보 조회', s == 200 and b['id'] == user['id'], b)
s, b = call('GET', '/api/users/me', headers={'X-Debug-User-Id': str(user['id'])})
check('X-Debug-User-Id (local 전용)', s == 200 and b['id'] == user['id'], b)

s, b = call('POST', '/api/verifications/email-code', token=token)
check('60초 내 재발송 → 429 TOO_MANY_REQUESTS', s == 429 and b['code'] == 'TOO_MANY_REQUESTS', b)
s, b = call('POST', '/api/verifications/email-code/confirm', {'emailCode': '000000x'}, token=token)
check('틀린 코드 → 400 remainingAttempts=4', s == 400 and b['detail'] == {'remainingAttempts': 4}, b)
code = None
if LOG:
    time.sleep(0.5)
    found = re.findall(re.escape(signup['email'].lower()) + r' → (\d+)', open(LOG, encoding='utf-8').read())
    code = found[-1] if found else None
s, b = call('POST', '/api/verifications/email-code/confirm', {'emailCode': code or '-'}, token=token)
check('맞는 코드 → VERIFIED (실패 횟수는 롤백되지 않고 남아 있었음)', s == 200 and b['emailStatus'] == 'VERIFIED', b)

png = b'\x89PNG\r\n\x1a\n' + b'\x00' * 64
boundary = 'X' + tag


def multipart(fields, file_field, filename, content):
    out = b''
    for k, v in fields.items():
        out += f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
    out += (f'--{boundary}\r\nContent-Disposition: form-data; name="{file_field}"; filename="{filename}"\r\n'
            f'Content-Type: image/png\r\n\r\n').encode() + content + b'\r\n'
    out += f'--{boundary}--\r\n'.encode()
    return out


mp = 'multipart/form-data; boundary=' + boundary
s, b = call('POST', '/api/verifications/capture', token=token, ctype=mp,
            raw=multipart({'studentNo': '9999'}, 'captureImage', 'a.png', png))
check('학번 불일치 → STUDENT_NO_MISMATCH', s == 400 and b['code'] == 'STUDENT_NO_MISMATCH', b)
s, b = call('POST', '/api/verifications/capture', token=token, ctype=mp,
            raw=multipart({'studentNo': signup['studentNo']}, 'captureImage', 'a.txt', b'hello world'))
check('이미지 아님 → INVALID_INPUT', s == 400 and b['code'] == 'INVALID_INPUT', b)
s, b = call('POST', '/api/verifications/capture', token=token, ctype=mp,
            raw=multipart({'studentNo': signup['studentNo']}, 'captureImage', 'a.png', png))
check('캡처 제출 → PENDING', s == 200 and b['reviewStatus'] == 'PENDING', b)
s, b = call('POST', '/api/verifications/capture', token=token, ctype=mp,
            raw=multipart({'studentNo': signup['studentNo']}, 'captureImage', 'a.png', png))
check('중복 제출 → 409 REVIEW_IN_PROGRESS', s == 409 and b['code'] == 'REVIEW_IN_PROGRESS', b)

pot = {'title': 't', 'departureName': 'a', 'departureLat': 37.5, 'departureLng': 127.0, 'destinationName': 'b',
       'destinationLat': 37.4, 'destinationLng': 127.1, 'departureTime': '2026-10-16T23:30:00', 'expectedFare': 10000}
s, b = call('POST', '/api/pots', pot, token=token)
check('오프셋 없는 일시(body) → INVALID_DATETIME_FORMAT + detail', s == 400 and b['code'] == 'INVALID_DATETIME_FORMAT'
      and b['detail'] == {'field': 'departureTime', 'value': '2026-10-16T23:30:00'}, b)
s, b = call('POST', '/api/pots', dict(pot, departureTime='2026-10-16T14:30:00Z'), token=token)
check('Z(UTC) 일시 → INVALID_DATETIME_FORMAT', s == 400 and b['code'] == 'INVALID_DATETIME_FORMAT', b)
s, b = call('POST', '/api/pots', dict(pot, departureTime='2026-10-16T23:30:00+09:00'), token=token)
check('미구현 API → 501 NOT_IMPLEMENTED', s == 501 and b['code'] == 'NOT_IMPLEMENTED', b)
s, b = call('GET', '/api/pots?from=2026-10-16T23:00:00+09:00', token=token)
check("쿼리 '+' 가 공백으로 깨져도 통과", s == 501, b)
s, b = call('GET', '/api/pots?from=2026-10-16T23:00:00', token=token)
check('쿼리 오프셋 없음 → INVALID_DATETIME_FORMAT field=from', s == 400 and b['detail'] == {'field': 'from', 'value': '2026-10-16T23:00:00'}, b)
s, b = call('GET', '/api/pots?sort=WRONG', token=token)
check('잘못된 enum 쿼리 → INVALID_INPUT', s == 400 and b['code'] == 'INVALID_INPUT', b)
s, b = call('GET', '/api/pots?page=0&size=20&minHostRating=11', token=token)
check('쿼리 범위 검증 → INVALID_INPUT', s == 400 and b['code'] == 'INVALID_INPUT', b)

s, b = call('GET', '/api/admin/users', token=token)
check('일반 회원 운영자 API → 403 FORBIDDEN', s == 403 and b['code'] == 'FORBIDDEN', b)
s, b = call('POST', '/api/auth/login', {'email': 'admin@taxipot.local', 'password': 'admin1234!'})
check('운영자 로그인 role=ADMIN', s == 200 and b['user']['role'] == 'ADMIN', b)
admin = b['accessToken']
s, b = call('GET', '/api/admin/users', token=admin)
check('운영자 API 통과 (미구현 501)', s == 501, b)
s, b = call('POST', '/api/auth/login', {'email': signup['email'], 'password': 'wrong-password'})
check('비밀번호 오류 → 401 INVALID_CREDENTIALS', s == 401 and b['code'] == 'INVALID_CREDENTIALS', b)
s, b = call('POST', '/api/auth/refresh', {'refreshToken': refresh})
check('토큰 재발급', s == 200 and b['accessToken'], b)
s, b = call('POST', '/api/auth/refresh', {'refreshToken': token})
check('액세스 토큰으로 재발급 시도 → 401', s == 401, b)

s, b = call('GET', '/api/notifications?page=0&size=5', token=token)
check('알림 목록 페이지 형식', s == 200 and b['page'] == {'page': 0, 'size': 5, 'totalElements': 0, 'totalPages': 0}, b)
s, b = call('GET', '/api/notifications?size=1000', token=token)
check('size 상한 초과 → INVALID_INPUT', s == 400, b)
s, b = call('GET', '/api/quick-blocks', token=token)
check('퀵블록 목록', s == 200 and b[0]['code'] == 'LATE_1', b)
s, b = call('GET', '/api/nope', token=token)
check('없는 API → 404 NOT_FOUND', s == 404 and b['code'] == 'NOT_FOUND', b)
s, b = call('GET', '/v3/api-docs')
check('Swagger 문서 (서버 구현)', s == 200, str(b)[:200])
s, b = call('GET', '/api-spec/taxipot-api.yaml')
check('Swagger 공식 명세 yaml', s == 200 and 'openapi' in str(b)[:50], str(b)[:100])

print(f"\n{sum(ok for _, ok in results)}/{len(results)} 통과")
print(json.dumps({'userId': user['id'], 'token': token, 'adminToken': admin}))
