package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.dto.PotCreateRequest;
import com.taxipot.domain.pot.dto.PotDetailResponse;
import com.taxipot.domain.pot.dto.PotPageResponse;
import com.taxipot.domain.pot.dto.PotSort;
import com.taxipot.domain.pot.dto.PotSummaryResponse;
import com.taxipot.domain.pot.dto.PotUpdateRequest;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.util.GeoUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class PotService {

    /** 위도 1도 ≈ 111,320m (반경 검색 1차 범위 계산용) */
    private static final double METERS_PER_DEGREE = 111_320d;
    private static final int COORD_SCALE = 7;

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final PotEligibilityValidator eligibilityValidator;
    private final PotPolicy potPolicy;
    private final PotDeposit potDeposit;
    private final PotExitHandler potExitHandler;
    private final PotMapper potMapper;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final TimeProvider timeProvider;

    /**
     * <b>GET /api/pots</b> — 택시팟 목록 조회 (지도 마커 / 바텀 시트 공용)
     * <p>
     * 기본값은 모집중(RECRUITING) 방만 조회.
     * - 참여 마감(joinDeadline = 집결 기한)이 지난 RECRUITING 방도 숨기지 않는다 (16일 회의). 앱은 joinDeadline과 응답 헤더 `Date`(서버 시각)를 비교해
     *   마감 전이면 '참여', 마감 후면 '입장 요청' 버튼을 보여준다 (폰 시계 오차 대비, 최종 판정은 서버)
     * - RECRUITING이면서 has_canceled = 1(참여자 이탈로 빈자리 발생)인 방은 정렬 조건과 관계없이 상단 노출, 응답 hasCanceled로 색상 구분
     * - 내 성별로 참여할 수 없는 성별 제한 방은 자동 제외
     * - lat·lng를 주면 반경(radius) 안의 방만 조회하고 distanceMeters를 채움 (지도 마커는 응답 좌표 사용)
     * - 출발지·도착지 이름 검색, 출발 시각 범위, 방장 평점 필터·정렬 지원
     * <p>
     * 구현: 조건(상태·이름·시각·성별·반경의 사각형 범위)은 DB 에서 거르고, 정확한 거리·방장 평점·정렬·페이지는 메모리에서 처리한다.
     * RECRUITING 방은 출발 24시간 이내라 수가 적고, 거리·방장 평점(USERS) 정렬을 DB 페이지와 함께 하기 어렵기 때문.
     */
    @Transactional(readOnly = true)
    public PotPageResponse getPots(Long userId, Double lat, Double lng, int radius, String departureName, String destinationName, OffsetDateTime from, OffsetDateTime to, GenderOption genderOption, Double minHostRating, PotSort sort, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.unsorted());   // page·size 범위 검사
        if ((lat == null) != (lng == null)) {
            throw Errors.invalidInput("lat 과 lng 는 함께 보내야 합니다.");
        }
        boolean hasPoint = lat != null;
        if (hasPoint) {
            validateCoordinate(lat, lng);
            if (radius < 1) {
                throw Errors.invalidInput("radius 는 1 이상이어야 합니다.");
            }
        }
        PotSort order = sort == null ? PotSort.DEPARTURE_TIME : sort;
        if (order == PotSort.DISTANCE && !hasPoint) {
            throw Errors.invalidInput("DISTANCE 정렬은 lat·lng 를 함께 보내야 합니다.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw Errors.invalidInput("from 은 to 보다 늦을 수 없습니다.");
        }

        User me = userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원"));
        Specification<Taxipot> spec = listSpec(me.getGender(), departureName, destinationName,
                from == null ? null : Kst.toLocal(from), to == null ? null : Kst.toLocal(to),
                genderOption, hasPoint ? lat : null, hasPoint ? lng : null, radius);
        List<Taxipot> candidates = taxipotRepository.findAll(spec);

        Map<Long, User> hosts = potMapper.usersById(candidates.stream().map(Taxipot::getHostId).toList());
        Map<Long, Integer> distances = new HashMap<>();
        List<Taxipot> filtered = new ArrayList<>();
        for (Taxipot pot : candidates) {
            if (hasPoint) {
                int distance = GeoUtils.distanceMeters(pot.getDepartureLat(), pot.getDepartureLng(), lat, lng);
                if (distance > radius) {
                    continue;
                }
                distances.put(pot.getId(), distance);
            }
            if (minHostRating != null) {
                User host = hosts.get(pot.getHostId());
                if (host == null || host.getRatingAvg().doubleValue() < minHostRating) {
                    continue;
                }
            }
            filtered.add(pot);
        }

        filtered.sort(listOrder(order, distances, hosts));

        int total = filtered.size();
        int fromIndex = (int) Math.min(pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<PotSummaryResponse> content = filtered.subList(fromIndex, toIndex).stream()
                .map(p -> potMapper.toSummary(p, hosts.get(p.getHostId()), distances.get(p.getId())))
                .toList();
        Page<PotSummaryResponse> result = new PageImpl<>(content, pageable, total);
        return new PotPageResponse(result.getContent(), PageMeta.from(result));
    }

    /**
     * <b>POST /api/pots</b> — 택시팟 방 생성
     * <p>
     * 하나의 트랜잭션으로 처리한다. 락 순서: USERS(본인) → WALLET(본인) (공통 규칙 '동시성 제어' 참고)
     * 1. TAXIPOT 생성 (status = RECRUITING, current_members = 1, arrival_deadline = departureTime − 5분)
     * 2. TAXIPOT_MEMBER에 방장 등록 (role = HOST)
     * 3. 방장 예치: WALLET.balance → held_balance, SETTLEMENT 생성(HELD, fee_amount NULL), POINT_HISTORY FARE_HOLD(−예치액) 1행
     * - 공통 이용 조건(인증·정지·미수금·잔액) 검증
     * - 출발 시각은 현재 + 15분 이후 ~ 현재 + 24시간 이내 (서버 설정값). 15분 미만이면 400 DEPARTURE_TIME_TOO_SOON (detail.minDepartureTime),
     *   24시간 초과면 400 DEPARTURE_TIME_TOO_FAR (detail.maxDepartureTime) (3.4.0)
     * - 성별 제한 방(MALE/FEMALE)은 방장 성별과 일치해야 생성 가능
     * - 이미 진행 중인 다른 방(핵심 정책 정의, 청구 접수 이후 방은 제외)에 참여 중이면 생성 불가
     * - 출발지·도착지 명칭은 카카오맵 핀 위치의 장소명 그대로 전달
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT (maxMembers가 3·4가 아님 등) / DEPARTURE_TIME_TOO_SOON (출발 15분 전보다 늦게 생성) / DEPARTURE_TIME_TOO_FAR (24시간 넘게 남은 출발 시각)
     * - 403: NOT_VERIFIED / SUSPENDED_USER / GENDER_RESTRICTED
     * - 409: INSUFFICIENT_BALANCE / ACTIVE_POT_EXISTS / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public PotDetailResponse createPot(Long userId, PotCreateRequest request) {
        // 입력값(400) 먼저 — 락을 잡기 전에 거른다
        validateCoordinate(request.departureLat(), request.departureLng());
        validateCoordinate(request.destinationLat(), request.destinationLng());
        if (request.title().isBlank()) {
            throw Errors.invalidInput("방 제목을 입력해 주세요.");
        }
        LocalDateTime now = timeProvider.now();
        LocalDateTime departure = Kst.toLocal(request.departureTime());
        LocalDateTime min = potPolicy.minDepartureTime(now);
        if (departure.isBefore(min)) {
            throw Errors.departureTimeTooSoon(min, potPolicy.minLeadMinutes());
        }
        LocalDateTime max = potPolicy.maxDepartureTime(now);
        if (departure.isAfter(max)) {
            throw Errors.departureTimeTooFar(max, potPolicy.maxAdvanceHours());
        }

        // 1. USERS 2. WALLET — 같은 회원이 동시에 방 두 개를 만드는 것도 여기서 막힌다
        LockedMember me = memberWalletLocker.lock(userId);
        int holdAmount = potPolicy.holdAmount(request.expectedFare(), request.maxMembers());
        GenderOption genderOption = request.genderOption() == null ? GenderOption.ALL : request.genderOption();
        eligibilityValidator.validate(me.user(), me.wallet(), null, genderOption, holdAmount);

        Taxipot pot = taxipotRepository.save(Taxipot.create(userId, request.title(),
                request.departureName(), coord(request.departureLat()), coord(request.departureLng()),
                request.destinationName(), coord(request.destinationLat()), coord(request.destinationLng()),
                departure, potPolicy.arrivalDeadline(departure),
                request.maxMembers(), genderOption, request.expectedFare(), blankToNull(request.openChatUrl())));
        memberRepository.save(TaxipotMember.host(pot.getId(), userId, request.preferredSeat(), now));
        potDeposit.hold(me.wallet(), pot.getId(), holdAmount);

        return potMapper.toDetail(pot, userId);
    }

    /**
     * <b>GET /api/pots/{potId}</b> — 택시팟 상세 조회
     * <p>
     * 방 정보와 참여자 목록(JOINED·BOARDED)을 함께 반환.
     * - 입장 요청 대기자(REQUESTED)는 members에 넣지 않는다. 방장은 `GET /api/pots/{potId}/join-requests`로 조회
     * - myMemberStatus로 요청자의 참여 상태(REQUESTED·REJECTED 포함)를 알 수 있다 (참여 이력 없으면 null)
     * - 오픈채팅 링크는 참여자에게만 노출
     * - 출발지·도착지 이름과 좌표로 앱에서 카카오 T·우버 딥링크를 생성
     * <p>
     * <b>에러 코드</b>
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public PotDetailResponse getPot(Long userId, Long potId) {
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        return potMapper.toDetail(pot, userId);
    }

    /**
     * <b>PATCH /api/pots/{potId}</b> — 택시팟 정보 수정 (방장 전용)
     * <p>
     * RECRUITING·CLOSED 상태에서 방 제목과 오픈채팅 링크만 수정 가능. 변경할 항목만 전달
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     * - 409: INVALID_POT_STATUS
     */
    @Transactional
    public PotDetailResponse updatePot(Long userId, Long potId, PotUpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) {
            throw Errors.invalidInput("방 제목을 입력해 주세요.");
        }
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!pot.isHost(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }
        requireBeforeStart(pot);

        pot.updateInfo(request.title(), request.openChatUrl());
        potRealtimePublisher.potUpdated(potId);
        return potMapper.toDetail(pot, userId);
    }

    /**
     * <b>DELETE /api/pots/{potId}</b> — 택시팟 방 취소 (방장 전용)
     * <p>
     * RECRUITING·CLOSED 상태에서 출발 10분 전까지만 가능. Body 없음.
     * - TAXIPOT status = CANCELED
     * - 전 참여자 예치금 전액 환불 (SETTLEMENT → REFUNDED, POINT_HISTORY FARE_REFUND)
     * - 입장 요청 대기자(REQUESTED) → CANCELED, canceled_at = now
     * - 참여자 전원·요청자에게 POT 알림
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     * - 409: INVALID_POT_STATUS / CANCEL_TIME_EXPIRED
     */
    @Transactional
    public void cancelPot(Long userId, Long potId) {
        // 권한은 락 없이 먼저 확인 (방장은 바뀌지 않는다) — 남의 방이면 아무것도 잠그지 않는다
        Taxipot snapshot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!snapshot.isHost(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }

        // 여러 회원: USERS 전부 → WALLET 전부(id 오름차순) → TAXIPOT
        Set<Long> memberIds = activeMemberIds(potId);
        Map<Long, LockedMember> locked = new HashMap<>(memberWalletLocker.lockAll(memberIds));
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        requireBeforeStart(pot);
        LocalDateTime now = timeProvider.now();
        LocalDateTime cancelDeadline = potPolicy.cancelDeadline(pot.getDepartureTime());
        if (!now.isBefore(cancelDeadline)) {
            throw Errors.cancelTimeExpired(cancelDeadline, potPolicy.cancelDeadlineMinutes());
        }

        // TAXIPOT 을 잠근 뒤 참여자를 다시 읽는다. 위에서 목록을 읽은 직후 참여해 커밋한 사람이 있으면 추가로 잠근다
        // (이제 TAXIPOT 락을 쥐고 있어 더 이상 참여자가 바뀌지 않는다)
        List<TaxipotMember> members = memberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, PotMapper.ACTIVE);
        Set<Long> missing = members.stream().map(TaxipotMember::getMemberId)
                .filter(id -> !locked.containsKey(id)).collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            locked.putAll(memberWalletLocker.lockAll(missing));
        }

        // 전원 환불 + 방 CANCELED + 입장 요청 대기자 CANCELED (스케줄러 해산과 같은 처리, PotExitHandler)
        List<Long> requesterIds = potExitHandler.dissolve(pot, members, locked, now);

        List<Long> guestIds = members.stream().filter(m -> !m.isHost()).map(TaxipotMember::getMemberId).toList();
        notificationSender.sendAll(guestIds, potId, NotificationType.POT, "택시팟 취소",
                "방장이 '%s' 방을 취소했어요. 예치금은 전액 환불되었습니다.".formatted(pot.getTitle()));
        notificationSender.sendAll(requesterIds, potId, NotificationType.POT, "입장 요청 취소",
                "입장을 요청한 '%s' 방이 취소되었어요.".formatted(pot.getTitle()));
        potRealtimePublisher.potUpdated(potId);
    }

    /**
     * <b>POST /api/pots/{potId}/start</b> — 탑승 시작 (방장 전용)
     * <p>
     * 방장이 택시 탑승 시 호출. RECRUITING·CLOSED 상태에서만 가능.
     * - 방장 포함 남은 JOINED 참여자 전원이 도착 확인(arrived_at NOT NULL)해야 함 — 한 명이라도 미도착이면 출발 불가 (16일 회의)
     *   (집결 기한 후 미도착자는 스케줄러가 이미 퇴장 처리, 방장은 기다리지 않고 내보내기도 가능)
     * - 탑승 인원이 2명 미만이면 시작 불가 (방장 혼자 남은 방은 출발 시각에 스케줄러가 자동 취소·환불)
     * - TAXIPOT status = RUNNING, started_at = now(청구 기한 기준, 3.5.0), 참여자 status = BOARDED, 입장 요청 대기자(REQUESTED) → CANCELED + POT 알림
     * - 이후 앱은 상세 응답 좌표로 외부 택시 앱 딥링크를 연다
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     * - 409: INVALID_POT_STATUS / NOT_ALL_ARRIVED / NOT_ENOUGH_MEMBERS
     */
    @Transactional
    public PotDetailResponse startPot(Long userId, Long potId) {
        // 지갑을 바꾸지 않으므로 TAXIPOT 만 잠근다 (참여·취소·내보내기와 순서가 섞이지 않음)
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!pot.isHost(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }
        requireBeforeStart(pot);

        List<TaxipotMember> members = memberRepository.findAllByPotIdAndStatus(potId, PotMemberStatus.JOINED);
        if (members.size() < 2) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_MEMBERS);
        }
        if (members.stream().anyMatch(m -> !m.hasArrived())) {
            throw new BusinessException(ErrorCode.NOT_ALL_ARRIVED);
        }

        LocalDateTime now = timeProvider.now();
        pot.start(now);
        members.forEach(TaxipotMember::board);
        List<Long> requesterIds = potExitHandler.cancelPendingRequests(potId, now);
        notificationSender.sendAll(requesterIds, potId, NotificationType.POT, "입장 요청 취소",
                "'%s' 방이 탑승을 시작해 입장 요청이 취소되었어요.".formatted(pot.getTitle()));

        potRealtimePublisher.potUpdated(potId);
        return potMapper.toDetail(pot, userId);
    }

    // ===== private =====

    /** RECRUITING·CLOSED 가 아니면 409 INVALID_POT_STATUS (수정·취소·탑승 시작 공통) */
    private static void requireBeforeStart(Taxipot pot) {
        if (pot.getStatus() != PotStatus.RECRUITING && pot.getStatus() != PotStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }
    }

    private Set<Long> activeMemberIds(Long potId) {
        return memberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, PotMapper.ACTIVE).stream()
                .map(TaxipotMember::getMemberId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 목록 DB 조건: RECRUITING, 내 성별로 참여 가능한 방, 이름 부분 일치, 출발 시각 범위, 성별 필터,
     * 반경 검색이면 기준점 주변 사각형 (정확한 원 판정은 메모리에서 하버사인으로).
     */
    private static Specification<Taxipot> listSpec(Gender myGender, String departureName, String destinationName,
                                                   LocalDateTime from, LocalDateTime to, GenderOption genderOption,
                                                   Double lat, Double lng, int radius) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), PotStatus.RECRUITING));
            predicates.add(root.get("genderOption").in(allowedGenderOptions(myGender)));
            if (genderOption != null) {
                predicates.add(cb.equal(root.get("genderOption"), genderOption));
            }
            if (departureName != null && !departureName.isBlank()) {
                predicates.add(contains(cb, root.get("departureName"), departureName));
            }
            if (destinationName != null && !destinationName.isBlank()) {
                predicates.add(contains(cb, root.get("destinationName"), destinationName));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("departureTime"), to));
            }
            if (lat != null) {
                double dLat = radius / METERS_PER_DEGREE;
                double cos = Math.max(Math.cos(Math.toRadians(lat)), 0.01);
                double dLng = radius / (METERS_PER_DEGREE * cos);
                predicates.add(cb.between(root.get("departureLat"),
                        BigDecimal.valueOf(lat - dLat), BigDecimal.valueOf(lat + dLat)));
                predicates.add(cb.between(root.get("departureLng"),
                        BigDecimal.valueOf(lng - dLng), BigDecimal.valueOf(lng + dLng)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 내 성별로 참여할 수 있는 성별 제한 값 (USERS.gender M ↔ MALE, F ↔ FEMALE, 성별 없음 → ALL 만) */
    private static List<GenderOption> allowedGenderOptions(Gender gender) {
        if (gender == Gender.M) {
            return List.of(GenderOption.ALL, GenderOption.MALE);
        }
        if (gender == Gender.F) {
            return List.of(GenderOption.ALL, GenderOption.FEMALE);
        }
        return List.of(GenderOption.ALL);
    }

    /** 부분 일치 검색. 입력의 %·_ 는 문자 그대로 찾는다 */
    private static Predicate contains(CriteriaBuilder cb, Expression<String> path, String keyword) {
        String escaped = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(path, "%" + escaped + "%", '\\');
    }

    /** 빈자리(hasCanceled) 방이 항상 위, 그다음 정렬 기준, 같으면 출발 시각·id 순 */
    private static Comparator<Taxipot> listOrder(PotSort sort, Map<Long, Integer> distances, Map<Long, User> hosts) {
        Comparator<Taxipot> canceledFirst = Comparator.comparing((Taxipot p) -> !Boolean.TRUE.equals(p.getHasCanceled()));
        Comparator<Taxipot> byDeparture = Comparator.comparing(Taxipot::getDepartureTime).thenComparing(Taxipot::getId);
        Comparator<Taxipot> key = switch (sort) {
            case DEPARTURE_TIME -> byDeparture;
            case DISTANCE -> Comparator.comparing((Taxipot p) -> distances.getOrDefault(p.getId(), Integer.MAX_VALUE))
                    .thenComparing(byDeparture);
            case HOST_RATING -> Comparator.comparing((Taxipot p) -> hostRating(hosts.get(p.getHostId())),
                    Comparator.reverseOrder()).thenComparing(byDeparture);
        };
        return canceledFirst.thenComparing(key);
    }

    private static BigDecimal hostRating(User host) {
        return host == null || host.getRatingAvg() == null ? BigDecimal.ZERO : host.getRatingAvg();
    }

    private static void validateCoordinate(Double lat, Double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw Errors.invalidInput("좌표 범위가 올바르지 않습니다.");
        }
    }

    /** DECIMAL(10·11, 7) 에 맞춰 소수 7자리로 */
    private static BigDecimal coord(Double value) {
        return BigDecimal.valueOf(value).setScale(COORD_SCALE, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
