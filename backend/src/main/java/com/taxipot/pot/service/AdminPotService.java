package com.taxipot.pot.service;

import com.taxipot.global.code.PotStatus;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.MyPotHistoryResponse;
import com.taxipot.pot.dto.PotSummaryResponse;
import com.taxipot.pot.dto.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/**
 * AdminPotController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPotService {

    /**
     * GET /api/admin/users/{userId}/pots — 회원 이용 기록 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 앱의 '내 택시 이용 기록'과 같은 응답 형식
     */
    public PageResponse<MyPotHistoryResponse> getUserPotHistoryPage(Long adminId, Long userId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/admin/users/{userId}/pots");
    }

    /**
     * GET /api/admin/pots — 전체 택시팟 이용 기록 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<PotSummaryResponse> getPotPage(Long adminId, PotStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/admin/pots");
    }

    /**
     * GET /api/admin/statistics — 매칭 현황 통계 (주요 출발지·선호 목적지·시간대별) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * TAXIPOT 집계 쿼리로 산출 (CANCELED 제외). 기간 미지정 시 최근 30일
     */
    public StatisticsResponse getStatistics(Long adminId, LocalDate from, LocalDate to, Integer limit) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/admin/statistics");
    }
}
