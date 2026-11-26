"""API 명세 → Controller(전부) + Service 스텁(담당자 구현용) 생성.
HAND 로 표시한 서비스는 손으로 작성하므로 만들지 않는다 (시그니처만 출력해 맞춘다)."""
import os
import re
import sys
import yaml

SPEC, OUT = sys.argv[1], sys.argv[2]
d = yaml.safe_load(open(SPEC, encoding='utf-8'))
S = d['components']['schemas']
CP = d['components']['parameters']
BASE = 'com.taxipot'

# (METHOD, path): (controller, service, method)
OPS = {
    ('POST', '/api/auth/signup'): ('auth.AuthController', 'auth.AuthService', 'signup'),
    ('GET', '/api/auth/nickname-check'): ('auth.AuthController', 'auth.AuthService', 'checkNickname'),
    ('GET', '/api/universities'): ('auth.AuthController', 'auth.AuthService', 'getUniversities'),
    ('GET', '/api/banks'): ('auth.AuthController', 'auth.AuthService', 'getBanks'),
    ('POST', '/api/auth/login'): ('auth.AuthController', 'auth.AuthService', 'login'),
    ('POST', '/api/auth/refresh'): ('auth.AuthController', 'auth.AuthService', 'refresh'),
    ('POST', '/api/auth/logout'): ('auth.AuthController', 'auth.AuthService', 'logout'),

    ('POST', '/api/verifications/email-code'): ('verification.VerificationController', 'verification.VerificationService', 'resendEmailCode'),
    ('POST', '/api/verifications/email-code/confirm'): ('verification.VerificationController', 'verification.VerificationService', 'confirmEmailCode'),
    ('POST', '/api/verifications/capture'): ('verification.VerificationController', 'verification.VerificationService', 'submitCapture'),
    ('GET', '/api/verifications/me'): ('verification.VerificationController', 'verification.VerificationService', 'getMyVerification'),

    ('GET', '/api/users/me'): ('user.UserController', 'user.UserService', 'getMyProfile'),
    ('PATCH', '/api/users/me'): ('user.UserController', 'user.UserService', 'updateMyProfile'),
    ('POST', '/api/users/me/withdraw'): ('user.UserController', 'user.UserWithdrawFacade', 'withdraw'),
    ('PATCH', '/api/users/me/password'): ('user.UserController', 'user.UserService', 'changePassword'),
    ('GET', '/api/users/me/account'): ('user.UserController', 'user.UserService', 'getMyAccount'),
    ('PUT', '/api/users/me/account'): ('user.UserController', 'user.UserService', 'updateMyAccount'),
    ('PUT', '/api/users/me/fcm-token'): ('user.UserController', 'user.UserService', 'updateFcmToken'),
    ('GET', '/api/users/me/pots'): ('user.UserController', 'pot.MyPotHistoryService', 'getMyPots'),
    ('GET', '/api/users/{userId}'): ('user.UserController', 'user.UserService', 'getPublicProfile'),
    ('GET', '/api/users/me/reports'): ('report.ReportController', 'report.ReportService', 'getMyReports'),
    ('POST', '/api/reports'): ('report.ReportController', 'report.ReportService', 'createReport'),

    ('GET', '/api/wallet'): ('wallet.WalletController', 'wallet.WalletService', 'getWallet'),
    ('GET', '/api/wallet/history'): ('wallet.WalletController', 'wallet.WalletService', 'getHistory'),
    ('GET', '/api/wallet/debts'): ('wallet.WalletController', 'wallet.DebtService', 'getMyDebts'),
    ('POST', '/api/wallet/debts/{settlementId}/pay'): ('wallet.WalletController', 'wallet.DebtService', 'payDebt'),
    ('POST', '/api/wallet/withdrawals'): ('wallet.WalletController', 'wallet.WithdrawalService', 'requestWithdrawal'),
    ('GET', '/api/wallet/withdrawals'): ('wallet.WalletController', 'wallet.WithdrawalService', 'getMyWithdrawals'),
    ('POST', '/api/payments'): ('payment.PaymentController', 'payment.PaymentService', 'createPayment'),
    ('GET', '/api/payments'): ('payment.PaymentController', 'payment.PaymentService', 'getMyPayments'),
    ('POST', '/api/payments/confirm'): ('payment.PaymentController', 'payment.PaymentService', 'confirmPayment'),

    ('GET', '/api/pots'): ('pot.PotController', 'pot.PotService', 'getPots'),
    ('POST', '/api/pots'): ('pot.PotController', 'pot.PotService', 'createPot'),
    ('GET', '/api/pots/{potId}'): ('pot.PotController', 'pot.PotService', 'getPot'),
    ('PATCH', '/api/pots/{potId}'): ('pot.PotController', 'pot.PotService', 'updatePot'),
    ('DELETE', '/api/pots/{potId}'): ('pot.PotController', 'pot.PotService', 'cancelPot'),
    ('POST', '/api/pots/{potId}/start'): ('pot.PotController', 'pot.PotService', 'startPot'),
    ('POST', '/api/pots/{potId}/members'): ('pot.PotMemberController', 'pot.PotMemberService', 'join'),
    ('DELETE', '/api/pots/{potId}/members/me'): ('pot.PotMemberController', 'pot.PotMemberService', 'leave'),
    ('PATCH', '/api/pots/{potId}/members/me/seat'): ('pot.PotMemberController', 'pot.PotMemberService', 'changeSeat'),
    ('POST', '/api/pots/{potId}/members/me/arrival'): ('pot.PotMemberController', 'pot.PotMemberService', 'confirmArrival'),
    ('POST', '/api/pots/{potId}/members/{userId}/kick'): ('pot.PotMemberController', 'pot.PotMemberService', 'kick'),
    ('POST', '/api/pots/{potId}/join-requests'): ('pot.JoinRequestController', 'pot.JoinRequestService', 'createJoinRequest'),
    ('GET', '/api/pots/{potId}/join-requests'): ('pot.JoinRequestController', 'pot.JoinRequestService', 'getJoinRequests'),
    ('DELETE', '/api/pots/{potId}/join-requests/me'): ('pot.JoinRequestController', 'pot.JoinRequestService', 'withdrawJoinRequest'),
    ('POST', '/api/pots/{potId}/join-requests/{userId}/accept'): ('pot.JoinRequestController', 'pot.JoinRequestService', 'acceptJoinRequest'),
    ('POST', '/api/pots/{potId}/join-requests/{userId}/reject'): ('pot.JoinRequestController', 'pot.JoinRequestService', 'rejectJoinRequest'),

    ('POST', '/api/pots/{potId}/fare-claim'): ('settlement.SettlementController', 'settlement.FareClaimService', 'claimFare'),
    ('GET', '/api/pots/{potId}/settlement'): ('settlement.SettlementController', 'settlement.SettlementQueryService', 'getPotSettlement'),
    ('GET', '/api/pots/{potId}/receipt'): ('settlement.SettlementController', 'settlement.SettlementQueryService', 'getReceipt'),
    ('GET', '/api/pots/{potId}/ratings/targets'): ('rating.RatingController', 'rating.RatingService', 'getRatingTargets'),
    ('POST', '/api/pots/{potId}/ratings'): ('rating.RatingController', 'rating.RatingService', 'submitRatings'),
    ('GET', '/api/pots/{potId}/messages'): ('chat.ChatController', 'chat.ChatService', 'getMessages'),
    ('POST', '/api/pots/{potId}/messages'): ('chat.ChatController', 'chat.ChatService', 'sendMessage'),
    ('GET', '/api/quick-blocks'): ('chat.ChatController', 'chat.ChatService', 'getQuickBlocks'),
    ('GET', '/api/notifications'): ('notification.NotificationController', 'notification.NotificationService', 'getMyNotifications'),
    ('GET', '/api/notifications/unread-count'): ('notification.NotificationController', 'notification.NotificationService', 'getUnreadCount'),
    ('PATCH', '/api/notifications/{notificationId}/read'): ('notification.NotificationController', 'notification.NotificationService', 'markRead'),
    ('PATCH', '/api/notifications/read-all'): ('notification.NotificationController', 'notification.NotificationService', 'markAllRead'),

    ('GET', '/api/admin/verifications'): ('verification.AdminVerificationController', 'verification.AdminVerificationService', 'getVerifications'),
    ('GET', '/api/admin/verifications/{verificationId}'): ('verification.AdminVerificationController', 'verification.AdminVerificationService', 'getVerification'),
    ('PATCH', '/api/admin/verifications/{verificationId}'): ('verification.AdminVerificationController', 'verification.AdminVerificationService', 'reviewVerification'),
    ('GET', '/api/admin/verifications/{verificationId}/capture'): ('verification.AdminVerificationController', 'verification.AdminVerificationService', 'getCaptureImage'),
    ('GET', '/api/admin/users'): ('user.AdminUserController', 'user.AdminUserService', 'getUsers'),
    ('GET', '/api/admin/users/{userId}'): ('user.AdminUserController', 'user.AdminUserService', 'getUserDetail'),
    ('GET', '/api/admin/users/{userId}/pots'): ('user.AdminUserController', 'pot.MyPotHistoryService', 'getUserPotsForAdmin'),
    ('PATCH', '/api/admin/users/{userId}/status'): ('user.AdminUserController', 'user.AdminUserService', 'updateUserStatus'),
    ('PATCH', '/api/admin/users/{userId}/wallet'): ('wallet.AdminWalletController', 'wallet.AdminWalletService', 'adjustWallet'),
    ('GET', '/api/admin/reports'): ('report.AdminReportController', 'report.AdminReportService', 'getReports'),
    ('PATCH', '/api/admin/reports/{reportId}'): ('report.AdminReportController', 'report.AdminReportService', 'processReport'),
    ('GET', '/api/admin/pots'): ('pot.AdminPotController', 'pot.AdminPotService', 'getPots'),
    ('GET', '/api/admin/fare-claims'): ('settlement.AdminFareClaimController', 'settlement.AdminFareClaimService', 'getFareClaims'),
    ('PATCH', '/api/admin/pots/{potId}/fare-claim'): ('settlement.AdminFareClaimController', 'settlement.AdminFareClaimService', 'reviewFareClaim'),
    ('GET', '/api/admin/pots/{potId}/receipt'): ('settlement.AdminFareClaimController', 'settlement.AdminFareClaimService', 'getReceipt'),
    ('POST', '/api/admin/pots/batch-status'): ('settlement.AdminFareClaimController', 'settlement.AdminBatchStatusService', 'applyBatchStatus'),
    ('GET', '/api/admin/statistics'): ('admin.StatisticsController', 'admin.StatisticsService', 'getStatistics'),
}

# 담당 (docs/CONVENTIONS.md 1장과 같게)
OWNER = {
    'auth': '공통·인증', 'verification.VerificationService': '공통·인증', 'notification': '공통·인증',
    'pot': '택시팟', 'chat': '택시팟',
    'wallet': '지갑·정산', 'payment': '지갑·정산', 'settlement': '지갑·정산',
    'user': '회원·운영', 'verification.AdminVerificationService': '회원·운영', 'report': '회원·운영',
    'rating': '회원·운영', 'admin': '회원·운영',
}
# 손으로 작성하는 서비스 (생성 안 함)
HAND = {'auth.AuthService', 'verification.VerificationService', 'notification.NotificationService'}

# 생성 스텁 중 바로 구현해 두는 메서드: service -> {fields, imports, bodies}
CUSTOM = {
    'chat.ChatService': {
        'imports': ['com.taxipot.domain.chat.catalog.QuickBlockCatalog'],
        'bodies': {'getQuickBlocks': 'return QuickBlockCatalog.BLOCKS;'},
    },
    'user.UserService': {
        'fields': ['private final UserRepository userRepository;'],
        'imports': ['com.taxipot.domain.user.repository.UserRepository', 'com.taxipot.domain.user.mapper.UserMapper',
                    'com.taxipot.global.error.Errors'],
        'bodies': {
            'getMyProfile': 'return UserMapper.toMyProfile(userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원")));',
            'updateFcmToken': 'userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원")).changeFcmToken(request.fcmToken());',
        },
    },
}


def owner_of(svc):
    return OWNER.get(svc) or OWNER[svc.split('.')[0]]


def schema_pkg(name):
    for root, dirs, files in os.walk(os.path.join(OUT, 'domain')):
        if name + '.java' in files and root.endswith('dto'):
            return BASE + '.' + os.path.relpath(root, OUT).replace(os.sep, '.')
    gp = os.path.join(OUT, 'global', 'code', name + '.java')
    if os.path.exists(gp):
        return BASE + '.global.code'
    raise KeyError(name)


def ref(s):
    return s['$ref'].split('/')[-1]


def java_type(schema, imports):
    if '$ref' in schema:
        n = ref(schema)
        imports.add(f'{schema_pkg(n)}.{n}')
        return n
    t, f = schema.get('type'), schema.get('format')
    if t == 'array':
        imports.add('java.util.List')
        return f'List<{java_type(schema["items"], imports)}>'
    if t == 'string' and f == 'date-time':
        imports.add('java.time.OffsetDateTime')
        return 'OffsetDateTime'
    if t == 'string' and f == 'date':
        imports.add('java.time.LocalDate')
        return 'LocalDate'
    if t == 'string' and 'enum' in schema:
        imports.add(f'{BASE}.domain.pot.dto.PotSort')
        return 'PotSort'
    if t == 'string':
        return 'String'
    if t == 'integer':
        return 'Long' if f == 'int64' else 'Integer'
    if t == 'number':
        return 'Double'
    if t == 'boolean':
        return 'Boolean'
    raise ValueError(schema)


def javadoc_text(op):
    parts = []
    parts.append(op.get('summary', ''))
    desc = (op.get('description') or '').strip()
    if desc:
        parts.append('')
        parts += desc.splitlines()
    errs = []
    for code, r in op['responses'].items():
        if code.startswith('2'):
            continue
        if '$ref' in r:
            name = ref(r)
            errs.append(f'{code}: ' + {'BadRequest': 'INVALID_INPUT / INVALID_DATETIME_FORMAT', 'Unauthorized': 'UNAUTHORIZED',
                                        'Forbidden': 'FORBIDDEN', 'NotFound': 'NOT_FOUND'}.get(name, name))
        else:
            errs.append(f'{code}: {r.get("description", "").strip()}')
    if errs:
        parts.append('')
        parts.append('<b>에러 코드</b>')
        parts += ['- ' + e for e in errs]
    out = []
    for line in parts:
        line = line.replace('*/', '*&#47;').replace('<=', '&lt;=').replace('>=', '&gt;=')
        line = re.sub(r'(?<![<\w])<(?![/bp]\b|b>|/b>)', '&lt;', line)
        line = line.replace('→', '→')
        out.append(line)
    return out


def emit(path, src):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)


controllers = {}
services = {}
signatures = []

for path, item in d['paths'].items():
    for m, op in item.items():
        if m not in ('get', 'post', 'put', 'patch', 'delete'):
            continue
        M = m.upper()
        ctrl, svc, fn = OPS[(M, path)]
        cimp, simp = set(), set()
        public = op.get('security') == []
        c_params, s_params, call_args = [], [], []
        if not public:
            # 로그인 회원 이름: 운영자 API = adminId, 경로에 {userId}(대상 회원)가 있으면 requesterId, 그 외 userId
            login_name = 'adminId' if path.startswith('/api/admin') else ('requesterId' if '{userId}' in path else 'userId')
            c_params.append('@LoginUserId Long ' + login_name)
            s_params.append('Long ' + login_name)
            call_args.append(login_name)
        params = [CP[ref(p)] if '$ref' in p else p for p in op.get('parameters', [])]
        for p in params:
            sch = p['schema']
            name = p['name']
            jt = java_type(sch, cimp)
            java_type(sch, simp)
            if p['in'] == 'path':
                c_params.append(f'@PathVariable Long {name}')
                s_params.append(f'Long {name}')
            else:
                ann = []
                default = sch.get('default')
                if default is not None:
                    dv = str(default).lower() if isinstance(default, bool) else str(default)
                    ann.append(f'@RequestParam(defaultValue = "{dv}")')
                    if jt == 'Integer':
                        jt = 'int'
                    elif jt == 'Boolean':
                        jt = 'boolean'
                elif p.get('required'):
                    ann.append('@RequestParam')
                else:
                    ann.append('@RequestParam(required = false)')
                if jt == 'LocalDate':
                    ann.append('@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)')
                    cimp.add('org.springframework.format.annotation.DateTimeFormat')
                if 'maxLength' in sch:
                    ann.append(f'@Size(max = {sch["maxLength"]})')
                    cimp.add('jakarta.validation.constraints.Size')
                if 'minimum' in sch and jt == 'Double':
                    ann.append(f'@DecimalMin("{sch["minimum"]}") @DecimalMax("{sch["maximum"]}")')
                    cimp.update(['jakarta.validation.constraints.DecimalMin', 'jakarta.validation.constraints.DecimalMax'])
                c_params.append(f'{" ".join(ann)} {jt} {name}')
                s_params.append(f'{jt} {name}')
            call_args.append(name)
        multipart = False
        if 'requestBody' in op:
            ct, body = next(iter(op['requestBody']['content'].items()))
            bt = java_type(body['schema'], cimp)
            java_type(body['schema'], simp)
            if ct == 'multipart/form-data':
                multipart = True
                c_params.append(f'@Valid @ModelAttribute {bt} request')
                cimp.add('org.springframework.web.bind.annotation.ModelAttribute')
            else:
                c_params.append(f'@Valid @RequestBody {bt} request')
                cimp.add('org.springframework.web.bind.annotation.RequestBody')
            cimp.add('jakarta.validation.Valid')
            s_params.append(f'{bt} request')
            call_args.append('request')
        ok_code = next(c for c in op['responses'] if c.startswith('2'))
        resp = op['responses'][ok_code]
        if ok_code == '204' or ('$ref' in resp and ref(resp) == 'NoContent'):
            kind, ctype, stype = 'void', 'ResponseEntity<Void>', 'void'
            cimp.add('org.springframework.http.ResponseEntity')
        else:
            ct = next(iter(resp['content']))
            if ct.startswith('image/'):
                kind, ctype, stype = 'image', 'ResponseEntity<Resource>', 'StoredFile'
                cimp.update(['org.springframework.http.ResponseEntity', 'org.springframework.core.io.Resource',
                             f'{BASE}.global.file.ImageResponses'])
                simp.add(f'{BASE}.global.file.StoredFile')
            else:
                kind = 'json'
                ctype = stype = java_type(resp['content'][ct]['schema'], cimp)
                java_type(resp['content'][ct]['schema'], simp)
        if not public:
            cimp.add(f'{BASE}.global.security.LoginUserId')
        if any(p['in'] == 'path' for p in params):
            cimp.add('org.springframework.web.bind.annotation.PathVariable')
        if any(p['in'] == 'query' for p in params):
            cimp.add('org.springframework.web.bind.annotation.RequestParam')
        cimp.add(f'org.springframework.web.bind.annotation.{M.capitalize()}Mapping')

        svc_simple = svc.split('.')[1]
        svc_field = svc_simple[0].lower() + svc_simple[1:]
        call = f'{svc_field}.{fn}({", ".join(call_args)})'
        mapping_args = f'"{path}"'
        if multipart:
            mapping_args = f'value = "{path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE'
            cimp.add('org.springframework.http.MediaType')
        cm = [f'    @Operation(summary = "{op.get("summary", "").replace(chr(34), chr(39))}")',
              f'    @{M.capitalize()}Mapping({mapping_args})',
              f'    public {ctype} {fn}({", ".join(c_params)}) {{']
        if kind == 'void':
            cm += [f'        {call};', '        return ResponseEntity.noContent().build();']
        elif kind == 'image':
            cm += [f'        return ImageResponses.of({call});']
        else:
            cm += [f'        return {call};']
        cm.append('    }')
        c = controllers.setdefault(ctrl, {'imports': set(), 'methods': [], 'services': {}, 'tags': set()})
        c['imports'] |= cimp
        c['methods'].append('\n'.join(cm))
        c['services'][svc] = (svc_simple, svc_field)
        c['tags'].update(op.get('tags', []))

        s = services.setdefault(svc, {'imports': set(), 'methods': []})
        s['imports'] |= simp
        readonly = M == 'GET'
        doc = javadoc_text(op)
        body_custom = CUSTOM.get(svc, {}).get('bodies', {}).get(fn)
        lines = ['    /**', f'     * <b>{M} {path}</b> — {doc[0]}']
        lines += [f'     * {l}' if l else '     * <p>' for l in doc[1:]]
        lines.append('     */')
        lines.append('    @Transactional' + ('(readOnly = true)' if readonly else ''))
        lines.append(f'    public {stype} {fn}({", ".join(s_params)}) {{')
        if body_custom:
            lines.append(f'        {body_custom}')
        else:
            lines.append(f'        // TODO({owner_of(svc)}): 위 명세대로 구현')
            lines.append(f'        throw new NotImplementedYetException("{M} {path}");')
        lines.append('    }')
        s['methods'].append('\n'.join(lines))
        signatures.append(f'{svc}: {stype} {fn}({", ".join(s_params)})')


def imports_block(imps, own_pkg):
    imps = sorted(i for i in imps if i.rsplit('.', 1)[0] != own_pkg)
    o = [i for i in imps if not i.startswith('java.')]
    j = [i for i in imps if i.startswith('java.')]
    out = [f'import {i};' for i in o]
    if o and j:
        out.append('')
    out += [f'import {i};' for i in j]
    return '\n'.join(out)


for ctrl, c in controllers.items():
    dom, name = ctrl.split('.')
    pkg = f'{BASE}.domain.{dom}.controller'
    imps = set(c['imports'])
    imps.update(['lombok.RequiredArgsConstructor', 'org.springframework.web.bind.annotation.RestController',
                 'io.swagger.v3.oas.annotations.Operation', 'io.swagger.v3.oas.annotations.tags.Tag'])
    fields = []
    for svc, (simple, field) in c['services'].items():
        sdom = svc.split('.')[0]
        imps.add(f'{BASE}.domain.{sdom}.service.{simple}')
        fields.append(f'    private final {simple} {field};')
    if any('@Size' in m or '@DecimalMin' in m for m in c['methods']):
        imps.add('org.springframework.validation.annotation.Validated')
    tag = '/'.join(sorted(c['tags']))
    src = [f'package {pkg};', '', imports_block(imps, pkg), '',
           '/**', f' * 명세 tag: {tag} — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:',
           ' * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.', ' */',
           '@Tag(name = "' + tag + '")', '@RestController', '@RequiredArgsConstructor']
    if 'org.springframework.validation.annotation.Validated' in imps:
        src.append('@Validated')
    src += [f'public class {name} {{', '', '\n'.join(fields), '', '\n\n'.join(c['methods']), '}', '']
    emit(os.path.join(OUT, 'domain', dom, 'controller', name + '.java'), '\n'.join(src))

for svc, s in services.items():
    if svc in HAND:
        continue
    dom, name = svc.split('.')
    pkg = f'{BASE}.domain.{dom}.service'
    custom = CUSTOM.get(svc, {})
    imps = set(s['imports']) | set(custom.get('imports', []))
    imps.update(['lombok.RequiredArgsConstructor', 'org.springframework.stereotype.Service',
                 'org.springframework.transaction.annotation.Transactional',
                 f'{BASE}.global.error.NotImplementedYetException'])
    fields = custom.get('fields', [])
    head = [f'package {pkg};', '', imports_block(imps, pkg), '',
            '/**', f' * 담당: {owner_of(svc)}', ' * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.',
            ' * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.', ' */',
            '@Service', '@RequiredArgsConstructor', f'public class {name} {{', '']
    if fields:
        head += ['    ' + f for f in fields] + ['']
    src = head + ['\n\n'.join(s['methods']), '}', '']
    emit(os.path.join(OUT, 'domain', dom, 'service', name + '.java'), '\n'.join(src))

print('controllers', len(controllers), 'services', len([s for s in services if s not in HAND]), 'ops', len(signatures))
for sig in signatures:
    if sig.split(':')[0] in HAND:
        print('HAND', sig)
