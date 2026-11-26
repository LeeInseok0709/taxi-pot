package com.taxipot.domain.user.service;

import com.taxipot.domain.user.dto.AdminUserDetailResponse;
import com.taxipot.domain.user.dto.AdminUserPageResponse;
import com.taxipot.domain.user.dto.AdminUserResponse;
import com.taxipot.domain.user.dto.UserStatusUpdateRequest;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    /**
     * <b>GET /api/admin/users</b> — 가입 회원 목록 조회
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsers(Long adminId, String keyword, UserStatus status, int page, int size) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/users");
    }

    /**
     * <b>GET /api/admin/users/{userId}</b> — 회원 상세 조회 (개인정보·지갑·캡처본·신고 이력 포함)
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long adminId, Long userId) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/users/{userId}");
    }

    /**
     * <b>PATCH /api/admin/users/{userId}/status</b> — 회원 이용 정지·해제 (수동)
     * <p>
     * - SUSPENDED: suspendedUntil 미지정 시 영구 정지
     *   - 정지 규칙 통일 (3.3.0): 이미 정지 중이면 기존 정지와 새 정지 중 더 긴 쪽 유지 (영구 > 기간제). 짧게 바꾸려면 먼저 ACTIVE로 해제 후 다시 정지
     * - ACTIVE: 정지 해제 (suspended_until = NULL). 영구 정지를 풀 수 있는 유일한 방법
     * - WITHDRAWN 회원은 변경 불가
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN / WITHDRAWN_USER
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long adminId, Long userId, UserStatusUpdateRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/admin/users/{userId}/status");
    }
}
