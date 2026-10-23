package com.taxipot.pot.dto;

import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PreferredSeat;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 PotMemberResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotMemberResponse(
        /** 참여 고유번호 (TAXIPOT_MEMBER.id) */
        Long id,
        Long memberId,
        String nickname,
        Double ratingAvg,
        PotMemberRole role,
        PreferredSeat preferredSeat,
        PotMemberStatus status,
        /** null 가능 */
        OffsetDateTime arrivedAt,
        OffsetDateTime joinedAt,
        /** 참여 취소·입장 요청 철회 시각, KICKED·NO_SHOW 퇴장 처리 시각 (3.3.0); null 가능 */
        OffsetDateTime canceledAt
) {}
