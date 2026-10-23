package com.taxipot.pot.dto;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 명세 3.9.0 스키마 PotDetailResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotDetailResponse(
        Long id,
        String title,
        Long hostId,
        String hostNickname,
        Double hostRatingAvg,
        String departureName,
        Double departureLat,
        Double departureLng,
        String destinationName,
        Double destinationLat,
        Double destinationLng,
        OffsetDateTime departureTime,
        /** 집결 기한 겸 일반 참여 마감 시각 (출발 5분 전, 3.3.0). 이후 참여는 입장 요청으로만 */
        OffsetDateTime arrivalDeadline,
        /** 참여 취소 가능 마감 시각 (출발 10분 전), 서버 계산 값 */
        OffsetDateTime cancelDeadline,
        Integer maxMembers,
        Integer currentMembers,
        GenderOption genderOption,
        Integer expectedFare,
        /** 참여 시 필요한 1인 예치액 = floor(expectedFare × 1.2 ÷ maxMembers) + 1,000 (잔액 부족 판단 기준, 3.7.0) */
        Integer holdAmount,
        /** null 가능 */
        Integer actualFare,
        PotStatus status,
        /** 참여자 이탈로 빈자리가 생긴 적이 있는 방 (방 전체 취소는 status = CANCELED) */
        Boolean hasCanceled,
        /** 참여자에게만 노출, 비참여자는 null; null 가능 */
        String openChatUrl,
        /** 요청자의 참여 여부 (JOINED·BOARDED) */
        Boolean isJoined,
        /** 요청자의 이 방 참여 상태 (REQUESTED·REJECTED 포함), 참여 이력 없으면 null (3.3.0); null 가능 */
        PotMemberStatus myMemberStatus,
        /** 결제내역 사진 등록 여부 (탑승자는 `/api/pots/{potId}/receipt`로 조회, 3.3.0). 정산 승인 후 90일이 지나 파기되면 false (3.6.0) */
        Boolean hasReceipt,
        FareClaimStatus fareClaimStatus,
        /** 운영자 청구 반려 사유 (REJECTED일 때, 그 외 null, 3.5.0); null 가능 */
        String fareRejectReason,
        List<PotMemberResponse> members,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
