package com.taxipot.pot.service;

import com.taxipot.global.code.GenderOption;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.PotCreateRequest;
import com.taxipot.pot.dto.PotDetailResponse;
import com.taxipot.pot.dto.PotSort;
import com.taxipot.pot.dto.PotSummaryResponse;
import com.taxipot.pot.dto.PotUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

/**
 * PotController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotService {

    /**
     * GET /api/pots — 택시팟 목록 조회 (지도 마커 / 바텀 시트 공용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 기본값은 모집중(RECRUITING) 방만 조회.
     * - 참여 마감(joinDeadline = 집결 기한)이 지난 RECRUITING 방도 숨기지 않는다 (16일 회의). 앱은 joinDeadline과 응답 헤더 `Date`(서버 시각)를 비교해
     *   마감 전이면 '참여', 마감 후면 '입장 요청' 버튼을 보여준다 (폰 시계 오차 대비, 최종 판정은 서버)
     * - RECRUITING이면서 has_canceled = 1(참여자 이탈로 빈자리 발생)인 방은 정렬 조건과 관계없이 상단 노출, 응답 hasCanceled로 색상 구분
     * - 내 성별로 참여할 수 없는 성별 제한 방은 자동 제외
     * - lat·lng를 주면 반경(radius) 안의 방만 조회하고 distanceMeters를 채움 (지도 마커는 응답 좌표 사용)
     * - 출발지·도착지 이름 검색, 출발 시각 범위, 방장 평점 필터·정렬 지원
     */
    public PageResponse<PotSummaryResponse> getPotPage(Long userId, Double lat, Double lng, Integer radius, String departureName, String destinationName, OffsetDateTime from, OffsetDateTime to, GenderOption genderOption, Double minHostRating, PotSort sort, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/pots");
    }

    /**
     * POST /api/pots — 택시팟 방 생성 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
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
     */
    @Transactional
    public PotDetailResponse createPot(Long userId, PotCreateRequest request) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots");
    }

    /**
     * GET /api/pots/{potId} — 택시팟 상세 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 방 정보와 참여자 목록(JOINED·BOARDED)을 함께 반환.
     * - 입장 요청 대기자(REQUESTED)는 members에 넣지 않는다. 방장은 `GET /api/pots/{potId}/join-requests`로 조회
     * - myMemberStatus로 요청자의 참여 상태(REQUESTED·REJECTED 포함)를 알 수 있다 (참여 이력 없으면 null)
     * - 오픈채팅 링크는 참여자에게만 노출
     * - 출발지·도착지 이름과 좌표로 앱에서 카카오 T·우버 딥링크를 생성
     */
    public PotDetailResponse getPotDetail(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/pots/{potId}");
    }

    /**
     * PATCH /api/pots/{potId} — 택시팟 정보 수정 (방장 전용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * RECRUITING·CLOSED 상태에서 방 제목과 오픈채팅 링크만 수정 가능. 변경할 항목만 전달
     */
    @Transactional
    public PotDetailResponse updatePot(Long userId, Long potId, PotUpdateRequest request) {
        throw new UnsupportedOperationException("TODO(서연): PATCH /api/pots/{potId}");
    }

    /**
     * DELETE /api/pots/{potId} — 택시팟 방 취소 (방장 전용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * RECRUITING·CLOSED 상태에서 출발 10분 전까지만 가능. Body 없음.
     * - TAXIPOT status = CANCELED
     * - 전 참여자 예치금 전액 환불 (SETTLEMENT → REFUNDED, POINT_HISTORY FARE_REFUND)
     * - 입장 요청 대기자(REQUESTED) → CANCELED, canceled_at = now
     * - 참여자 전원·요청자에게 POT 알림
     */
    @Transactional
    public void cancelPot(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): DELETE /api/pots/{potId}");
    }

    /**
     * POST /api/pots/{potId}/start — 탑승 시작 (방장 전용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 방장이 택시 탑승 시 호출. RECRUITING·CLOSED 상태에서만 가능.
     * - 방장 포함 남은 JOINED 참여자 전원이 도착 확인(arrived_at NOT NULL)해야 함 — 한 명이라도 미도착이면 출발 불가 (16일 회의)
     *   (집결 기한 후 미도착자는 스케줄러가 이미 퇴장 처리, 방장은 기다리지 않고 내보내기도 가능)
     * - 탑승 인원이 2명 미만이면 시작 불가 (방장 혼자 남은 방은 출발 시각에 스케줄러가 자동 취소·환불)
     * - TAXIPOT status = RUNNING, started_at = now(청구 기한 기준, 3.5.0), 참여자 status = BOARDED, 입장 요청 대기자(REQUESTED) → CANCELED + POT 알림
     * - 이후 앱은 상세 응답 좌표로 외부 택시 앱 딥링크를 연다
     */
    @Transactional
    public PotDetailResponse startPot(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/start");
    }
}
