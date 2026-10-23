package com.taxipot.user.service;

import com.taxipot.global.code.UserStatus;
import com.taxipot.global.common.PageResponse;
import com.taxipot.user.dto.AdminUserDetailResponse;
import com.taxipot.user.dto.AdminUserResponse;
import com.taxipot.user.dto.UserStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminUserController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    /**
     * GET /api/admin/users — 가입 회원 목록 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<AdminUserResponse> getUserPage(Long adminId, String keyword, UserStatus status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/admin/users");
    }

    /**
     * GET /api/admin/users/{userId} — 회원 상세 조회 (개인정보·지갑·캡처본·신고 이력 포함) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public AdminUserDetailResponse getUserDetail(Long adminId, Long userId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/admin/users/{userId}");
    }

    /**
     * PATCH /api/admin/users/{userId}/status — 회원 이용 정지·해제 (수동) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * - SUSPENDED: suspendedUntil 미지정 시 영구 정지
     *   - 정지 규칙 통일 (3.3.0): 이미 정지 중이면 기존 정지와 새 정지 중 더 긴 쪽 유지 (영구 > 기간제). 짧게 바꾸려면 먼저 ACTIVE로 해제 후 다시 정지
     * - ACTIVE: 정지 해제 (suspended_until = NULL). 영구 정지를 풀 수 있는 유일한 방법
     * - WITHDRAWN 회원은 변경 불가
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long adminId, Long userId, UserStatusUpdateRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/admin/users/{userId}/status");
    }
}
