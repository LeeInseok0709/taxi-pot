"""API 명세서(OpenAPI yaml) → Java enum / DTO record 자동 생성.
명세가 바뀌면 이 스크립트를 다시 돌려서 DTO를 재생성한다 (손으로 고치지 않는다)."""
import re
import sys
import yaml

SPEC = sys.argv[1]
OUT = sys.argv[2]  # src/main/java/com/taxipot
BASE_PKG = 'com.taxipot'

d = yaml.safe_load(open(SPEC, encoding='utf-8'))
S = d['components']['schemas']
P = d['paths']

DOMAIN = {
    'auth': ['SignupRequest', 'LoginRequest', 'LoginResponse', 'TokenResponse', 'RefreshRequest',
             'AvailabilityResponse', 'UniversityResponse', 'BankResponse'],
    'verification': ['EmailCodeSendResponse', 'EmailCodeConfirmRequest', 'CaptureSubmitRequest',
                     'VerificationResponse', 'AdminVerificationResponse', 'AdminVerificationPageResponse',
                     'VerificationReviewRequest'],
    'user': ['MyProfileResponse', 'UserUpdateRequest', 'PasswordChangeRequest', 'FcmTokenRequest',
             'WithdrawRequest', 'UserPublicResponse', 'AccountResponse', 'AccountUpdateRequest',
             'MyPotHistoryResponse', 'MyPotHistoryPageResponse', 'AdminUserResponse', 'AdminUserPageResponse',
             'UserStatusUpdateRequest', 'AdminUserDetailResponse'],
    'wallet': ['WalletResponse', 'PointHistoryResponse', 'PointHistoryPageResponse', 'DebtResponse',
               'DebtPayResponse', 'WithdrawalRequest', 'WithdrawalResponse', 'WithdrawalPageResponse',
               'WalletAdjustRequest'],
    'payment': ['PaymentCreateRequest', 'PaymentConfirmRequest', 'PaymentResponse', 'PaymentPageResponse'],
    'pot': ['PotCreateRequest', 'PotUpdateRequest', 'PotSummaryResponse', 'PotDetailResponse', 'PotPageResponse',
            'PotMemberResponse', 'JoinRequest', 'JoinResponse', 'JoinRequestCreateRequest', 'JoinRequestResponse',
            'SeatChangeRequest', 'ArrivalRequest', 'ArrivalResponse'],
    'settlement': ['FareClaimRequest', 'FareClaimResponse', 'FareClaimPageResponse', 'FareClaimReviewRequest',
                   'SettlementResponse', 'PotSettlementResponse', 'BatchStatusItem', 'BatchStatusRequest'],
    'rating': ['RatingTargetResponse', 'RatingItem', 'RatingCreateRequest', 'RatingResponse'],
    'report': ['ReportCreateRequest', 'ReportResponse', 'ReportPageResponse', 'AdminReportResponse',
               'AdminReportPageResponse', 'ReportProcessRequest'],
    'chat': ['ChatMessageRequest', 'ChatMessageResponse', 'ChatMessagePageResponse', 'QuickBlockResponse'],
    'notification': ['NotificationResponse', 'NotificationPageResponse', 'UnreadCountResponse'],
    'admin': ['StatisticsResponse'],
}
HAND_WRITTEN = {'ErrorResponse', 'PageMeta'}
INLINE_ENUM = {
    ('FareClaimReviewRequest', 'decision'): 'FareClaimStatus',
    ('VerificationReviewRequest', 'reviewStatus'): 'ReviewStatus',
    ('PaymentCreateRequest', 'pgProvider'): 'PgProvider',
    ('ReportProcessRequest', 'status'): 'ReportStatus',
    ('UserStatusUpdateRequest', 'status'): 'UserStatus',
    ('AvailabilityResponse', 'reason'): 'nested:Reason',
}  # global 에 직접 작성

# 코드 정의 시트의 의미 (enum 상수 주석용)
CODE_MEANING = {}
for line in open(sys.argv[3], encoding='utf-8').read().splitlines()[4:]:
    t, c, v, m = [x.strip() for x in line.split('|')]
    CODE_MEANING.setdefault((t, c), {})[v] = m
ENUM_DB = {
    'Gender': ('USERS', 'gender'), 'UserRole': ('USERS', 'role'), 'UserStatus': ('USERS', 'status'),
    'EmailStatus': ('STUDENT_VERIFICATION', 'email_status'), 'ReviewStatus': ('STUDENT_VERIFICATION', 'review_status'),
    'GenderOption': ('TAXIPOT', 'gender_option'), 'PotStatus': ('TAXIPOT', 'status'),
    'FareClaimStatus': ('TAXIPOT', 'fare_claim_status'), 'PotMemberRole': ('TAXIPOT_MEMBER', 'role'),
    'PreferredSeat': ('TAXIPOT_MEMBER', 'preferred_seat'), 'PotMemberStatus': ('TAXIPOT_MEMBER', 'status'),
    'PgProvider': ('PAYMENT', 'pg_provider'), 'PaymentStatus': ('PAYMENT', 'status'),
    'PointType': ('POINT_HISTORY', 'type'), 'SettlementStatus': ('SETTLEMENT', 'status'),
    'DebtStatus': ('SETTLEMENT', 'debt_status'), 'ReportType': ('REPORT', 'type'), 'ReportStatus': ('REPORT', 'status'),
    'MsgType': ('CHAT_MESSAGE', 'msg_type'), 'NotificationType': ('NOTIFICATION', 'type'),
    'WithdrawalStatus': ('WITHDRAWAL', 'status'), 'TransferProvider': ('WITHDRAWAL', 'transfer_provider'),
}

schema_pkg = {}
for dom, names in DOMAIN.items():
    for n in names:
        schema_pkg[n] = f'{BASE_PKG}.domain.{dom}.dto'
enum_names = [n for n, s in S.items() if 'enum' in s and s.get('type') == 'string']
for n in enum_names:
    schema_pkg[n] = f'{BASE_PKG}.global.code'
schema_pkg['PageMeta'] = f'{BASE_PKG}.global.response'

missing = [n for n in S if n not in schema_pkg and n not in HAND_WRITTEN]
assert not missing, f'도메인 미지정 스키마: {missing}'

# ---- 요청용 스키마 판별 (requestBody 에서 도달 가능한 스키마) ----
request_schemas = set()


def walk_refs(schema, acc):
    if not isinstance(schema, dict):
        return
    if '$ref' in schema:
        n = schema['$ref'].split('/')[-1]
        if n not in acc:
            acc.add(n)
            walk_refs(S[n], acc)
    for k in ('items',):
        if k in schema:
            walk_refs(schema[k], acc)
    for v in (schema.get('properties') or {}).values():
        walk_refs(v, acc)
    for v in schema.get('allOf', []):
        walk_refs(v, acc)


for path, item in P.items():
    for m, op in item.items():
        if not isinstance(op, dict) or 'requestBody' not in op:
            continue
        for ct in op['requestBody']['content'].values():
            walk_refs(ct['schema'], request_schemas)
request_schemas = {n for n in request_schemas if n not in enum_names}


def ref_name(s):
    return s['$ref'].split('/')[-1]


def unwrap(prop):
    """allOf: [$ref] 형태 → $ref"""
    if 'allOf' in prop and len(prop['allOf']) == 1:
        merged = dict(prop['allOf'][0])
        for k, v in prop.items():
            if k != 'allOf':
                merged[k] = v
        return merged
    return prop


def singular(name):
    if name.endswith('ies'):
        return name[:-3] + 'y'
    if name.endswith('s'):
        return name[:-1]
    return name


def cap(s):
    return s[0].upper() + s[1:]


class Gen:
    def __init__(self, name, schema, pkg, is_request):
        self.name = name
        self.schema = schema
        self.pkg = pkg
        self.is_request = is_request
        self.imports = set()
        self.nested = []  # (name, schema)
        self.inline_enums = {}

    def jtype(self, prop, prop_name):
        prop = unwrap(prop)
        if '$ref' in prop:
            n = ref_name(prop)
            if schema_pkg.get(n) and schema_pkg[n] != self.pkg:
                self.imports.add(f'{schema_pkg[n]}.{n}')
            return n
        t = prop.get('type')
        f = prop.get('format')
        if t == 'string':
            if f == 'date-time':
                self.imports.add('java.time.OffsetDateTime')
                return 'OffsetDateTime'
            if f == 'date':
                self.imports.add('java.time.LocalDate')
                return 'LocalDate'
            if f == 'binary':
                self.imports.add('org.springframework.web.multipart.MultipartFile')
                return 'MultipartFile'
            if 'enum' in prop:
                # 인라인 enum → 같은 값을 포함하는 공용 enum 으로 받고, 허용 값은 서비스에서 검사
                key = (self.name, prop_name)
                if key in INLINE_ENUM:
                    en = INLINE_ENUM[key]
                    if en.startswith('nested:'):
                        en = en[len('nested:'):]
                        self.inline_enums[en] = [v for v in prop['enum'] if v is not None]
                        return en
                    assert set(v for v in prop['enum'] if v is not None) <= set(S[en]['enum'])
                    self.imports.add(f'{BASE_PKG}.global.code.{en}')
                    return en
                raise ValueError(f'인라인 enum 처리 필요: {self.name}.{prop_name} {prop["enum"]}')
            return 'String'
        if t == 'integer':
            return 'Long' if f == 'int64' else 'Integer'
        if t == 'number':
            return 'Double'
        if t == 'boolean':
            return 'Boolean'
        if t == 'array':
            self.imports.add('java.util.List')
            return f'List<{self.jtype(prop["items"], singular(prop_name))}>'
        if t == 'object':
            if prop.get('additionalProperties') and not prop.get('properties'):
                self.imports.add('java.util.Map')
                return 'Map<String, Object>'
            nested_name = cap(singular(prop_name))
            self.nested.append((nested_name, prop))
            return nested_name
        raise ValueError(f'알 수 없는 타입 {self.name}.{prop_name}: {prop}')

    def annotations(self, prop, prop_name, jt, required):
        if not self.is_request:
            return []
        prop = unwrap(prop)
        a = []
        if required:
            if jt == 'String':
                a.append('@NotBlank')
                self.imports.add('jakarta.validation.constraints.NotBlank')
            else:
                a.append('@NotNull')
                self.imports.add('jakarta.validation.constraints.NotNull')
        if jt == 'String' or jt.startswith('List<'):
            mn = prop.get('minLength', prop.get('minItems'))
            mx = prop.get('maxLength', prop.get('maxItems'))
            if mn is not None or mx is not None:
                args = []
                if mn is not None:
                    args.append(f'min = {mn}')
                if mx is not None:
                    args.append(f'max = {mx}')
                a.append(f'@Size({", ".join(args)})')
                self.imports.add('jakarta.validation.constraints.Size')
        if 'pattern' in prop:
            pat = prop['pattern'].replace('\\', '\\\\').replace('"', '\\"')
            a.append(f'@Pattern(regexp = "{pat}")')
            self.imports.add('jakarta.validation.constraints.Pattern')
        if prop.get('format') == 'email':
            a.append('@Email')
            self.imports.add('jakarta.validation.constraints.Email')
        if jt in ('Integer', 'Long'):
            if 'enum' in prop:
                vals = prop['enum']
                a.append(f'@Min({min(vals)})')
                a.append(f'@Max({max(vals)})')
                self.imports.update(['jakarta.validation.constraints.Min', 'jakarta.validation.constraints.Max'])
            else:
                if 'minimum' in prop:
                    a.append(f'@Min({int(prop["minimum"])})')
                    self.imports.add('jakarta.validation.constraints.Min')
                if 'maximum' in prop:
                    a.append(f'@Max({int(prop["maximum"])})')
                    self.imports.add('jakarta.validation.constraints.Max')
        if jt == 'Double':
            if 'minimum' in prop:
                a.append(f'@DecimalMin("{prop["minimum"]}")')
                self.imports.add('jakarta.validation.constraints.DecimalMin')
            if 'maximum' in prop:
                a.append(f'@DecimalMax("{prop["maximum"]}")')
                self.imports.add('jakarta.validation.constraints.DecimalMax')
        is_obj = ('$ref' in prop and ref_name(prop) not in enum_names) or prop.get('type') == 'object'
        is_obj_list = prop.get('type') == 'array' and (
            ('$ref' in prop['items'] and ref_name(prop['items']) not in enum_names) or prop['items'].get('type') == 'object')
        if is_obj or is_obj_list:
            a.append('@Valid')
            self.imports.add('jakarta.validation.Valid')
        return a

    def default_literal(self, prop, jt):
        prop = unwrap(prop)
        if 'default' not in prop:
            return None
        v = prop['default']
        if jt == 'Boolean':
            return 'Boolean.TRUE' if v else 'Boolean.FALSE'
        if jt == 'Integer':
            return str(v)
        if jt == 'Long':
            return f'{v}L'
        if jt == 'String':
            return f'"{v}"'
        if '$ref' in prop:
            return f'{jt}.{v}'
        return None

    def body(self, indent=''):
        sch = self.schema
        props = sch.get('properties') or {}
        req = set(sch.get('required') or [])
        comps = []
        defaults = []
        for pn, pv in props.items():
            jt = self.jtype(pv, pn)
            ann = self.annotations(pv, pn, jt, pn in req)
            pvu = unwrap(pv)
            if jt == 'Boolean' and pn.startswith('is'):
                # record 는 원래 컴포넌트 이름 그대로 직렬화하지만, Jackson 버전에 따른 is 제거를 원천 차단
                ann.insert(0, f'@JsonProperty("{pn}")')
                self.imports.add('com.fasterxml.jackson.annotation.JsonProperty')
            note = []
            if not self.is_request:
                if pn not in req:
                    note.append('명세 required 아님')
                if pvu.get('nullable'):
                    note.append('nullable')
            else:
                if pn not in req:
                    note.append('선택')
                dl = self.default_literal(pv, jt)
                if dl is not None:
                    note.append(f'기본값 {dl}')
                    defaults.append((pn, dl))
                if pvu.get('nullable'):
                    note.append('null 허용')
            if 'enum' in pvu and pvu.get('type') == 'string':
                note.append('허용 값: ' + ', '.join(str(v) for v in pvu['enum'] if v is not None) + ' (그 외는 서비스에서 INVALID_INPUT)' if self.is_request else '값: ' + ', '.join(str(v) for v in pvu['enum'] if v is not None))
            desc = (pvu.get('description') or '').strip().replace('\n', ' ')
            if desc:
                desc = re.sub(r'\s+', ' ', desc)
                if len(desc) > 90:
                    desc = desc[:88] + '…'
                note.append(desc)
            comps.append((pn, jt, ann, note))
        return comps, defaults

    def render(self):
        comps, defaults = self.body()
        lines = []
        desc = (self.schema.get('description') or '').strip()
        lines.append('/**')
        lines.append(f' * {self.name} — API 명세서 3.9.0 components.schemas.{self.name}')
        if desc:
            for dl in desc.splitlines():
                lines.append(f' * <p>{dl}')
        lines.append(' * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)')
        lines.append(' */')
        lines += self.render_record(self.name, comps, defaults, '')
        nested_src = []
        done = set()
        while self.nested:
            nn, ns = self.nested.pop(0)
            if nn in done:
                continue
            done.add(nn)
            sub = Gen(nn, ns, self.pkg, self.is_request)
            sub.imports = self.imports
            sub.nested = self.nested
            c2, d2 = sub.body()
            nested_src.append(c2 and self.render_record(nn, c2, d2, '    ', nested=True))
        for en, vals in self.inline_enums.items():
            nested_src.append([f'    public enum {en} {{', '        ' + ', '.join(vals), '    }'])
        if nested_src:
            # 중첩 record 를 본 record 안에 삽입
            close = lines.pop()  # '}'
            for ns in nested_src:
                lines.append('')
                lines += ns
            lines.append(close)
        header = [f'package {self.pkg};', '']
        imps = sorted(i for i in self.imports if not i.startswith(self.pkg + '.') or i.count('.') > self.pkg.count('.') + 1)
        jimps = [i for i in imps if i.startswith('java.')]
        oimps = [i for i in imps if not i.startswith('java.')]
        for i in oimps:
            header.append(f'import {i};')
        if oimps and jimps:
            header.append('')
        for i in jimps:
            header.append(f'import {i};')
        if imps:
            header.append('')
        return '\n'.join(header + lines) + '\n'

    def render_record(self, name, comps, defaults, ind, nested=False):
        out = []
        kw = 'public record' if not nested else 'public record'
        if not comps:
            out.append(f'{ind}{kw} {name}() {{')
            out.append(f'{ind}}}')
            return out
        out.append(f'{ind}{kw} {name}(')
        for i, (pn, jt, ann, note) in enumerate(comps):
            if note:
                out.append(f'{ind}        // {" / ".join(note)}')
            a = (' '.join(ann) + ' ') if ann else ''
            sep = ',' if i < len(comps) - 1 else ''
            out.append(f'{ind}        {a}{jt} {pn}{sep}')
        out.append(f'{ind}) {{')
        if defaults:
            out.append(f'{ind}    /** 명세 기본값: 앱이 값을 보내지 않으면(null) 기본값으로 채운다 */')
            out.append(f'{ind}    public {name} {{')
            for pn, dl in defaults:
                out.append(f'{ind}        if ({pn} == null) {{')
                out.append(f'{ind}            {pn} = {dl};')
                out.append(f'{ind}        }}')
            out.append(f'{ind}    }}')
        out.append(f'{ind}}}')
        return out


def write(pkg, name, src):
    import os
    path = os.path.join(OUT, *pkg.replace(BASE_PKG + '.', '').split('.'), name + '.java')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)


# ---- enum ----
for n in enum_names + ['TransferProvider']:
    if n == 'TransferProvider':
        vals = ['DUMMY']
        sdesc = 'WITHDRAWAL.transfer_provider (DB 전용 코드, 명세 응답에는 provider 문자열로 노출)'
    else:
        vals = S[n]['enum']
        sdesc = (S[n].get('description') or '').replace('\n', ' ')
    t, c = ENUM_DB[n]
    meaning = CODE_MEANING[(t, c)]
    assert list(meaning.keys()) == vals, f'{n}: 명세 {vals} vs DB {list(meaning.keys())}'
    lines = [f'package {BASE_PKG}.global.code;', '', '/**', f' * {t}.{c} 코드값 (DB 설계서 13차 \'코드 정의\' 시트와 1:1).',
             ' * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 \'코드값(enum) 확장 정책\')',
             ' * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑',
             ' */', f'public enum {n} {{']
    for i, v in enumerate(vals):
        sep = ',' if i < len(vals) - 1 else ''
        lines.append(f'    /** {meaning[v]} */')
        lines.append(f'    {v}{sep}')
    lines.append('}')
    write(f'{BASE_PKG}.global.code', n, '\n'.join(lines) + '\n')

# ---- DTO ----
count = 0
for n, s in S.items():
    if n in HAND_WRITTEN or n in enum_names:
        continue
    g = Gen(n, s, schema_pkg[n], n in request_schemas)
    write(schema_pkg[n], n, g.render())
    count += 1
print('enum', len(enum_names) + 1, 'dto', count, 'request', sorted(request_schemas))
