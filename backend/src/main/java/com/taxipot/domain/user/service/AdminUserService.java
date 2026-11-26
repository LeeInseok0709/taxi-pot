package com.taxipot.domain.user.service;

import com.taxipot.domain.user.dto.AdminUserPageResponse;
import com.taxipot.domain.user.dto.AdminUserResponse;
import com.taxipot.domain.user.dto.UserStatusUpdateRequest;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 담당: 회원·운영 — 운영자 회원 관리 (인석 구현)
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>회원 상세(GET /api/admin/users/{userId})는 지갑·인증·신고·이용 기록을 모두 모아야 해서,
 * 의존 방향(CONVENTIONS 5장: user 는 notification 만 호출 가능)을 지키려고 AdminUserDetailFacade 로 옮겼다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final TimeProvider timeProvider;

    /**
     * <b>GET /api/admin/users</b> — 가입 회원 목록 조회
     * <p>keyword: 이름·닉네임·이메일·학번 검색 / status: 상태 필터 (둘 다 선택). 최근 가입순.
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
     */
    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsers(Long adminId, String keyword, UserStatus status, int page, int size) {
        // 구현: 인석
        String like = (keyword == null || keyword.isBlank()) ? null : "%" + escapeLike(keyword.strip()) + "%";
        Page<User> rows = userRepository.searchForAdmin(like, status,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new AdminUserPageResponse(rows.getContent().stream().map(AdminUserService::toResponse).toList(),
                PageMeta.from(rows));
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
        // 구현: 인석
        // 회원 행을 잠근다: 스케줄러 5(정지 만료 해제)·신고 승인 정지와 동시에 바뀌어도 덮어쓰지 않게
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> Errors.notFound("회원"));
        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }

        if (request.status() == UserStatus.ACTIVE) {
            user.releaseSuspension();                                  // 영구 정지도 여기서만 풀린다
        } else if (request.status() == UserStatus.SUSPENDED) {
            LocalDateTime until = Kst.toLocal(request.suspendedUntil()); // null = 영구 정지
            if (until != null && !until.isAfter(timeProvider.now())) {
                throw Errors.invalidInput("정지 해제 시각은 현재보다 뒤여야 합니다.");
            }
            user.suspend(until);                                       // 기존 정지가 더 길면 User.suspend 가 기존 값을 남긴다
        } else {
            throw Errors.invalidInput("status 는 ACTIVE 또는 SUSPENDED 만 가능합니다.");
        }
        return toResponse(user);
    }

    /** User → 목록 한 줄 응답. 상세 응답(AdminUserDetailFacade)도 같은 값을 쓴다 */
    public static AdminUserResponse toResponse(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getNickname(),
                u.getUniversity(),
                u.getStudentNo(),
                u.getIsVerified(),
                u.getRatingAvg() == null ? null : u.getRatingAvg().doubleValue(),
                u.getNoShowCount(),
                u.getReportCount(),
                u.getRole(),
                u.getStatus(),
                Kst.toOffset(u.getSuspendedUntil()),
                Kst.toOffset(u.getWithdrawnAt()),
                Kst.toOffset(u.getCreatedAt()));
    }

    /** LIKE 특수문자를 글자 그대로 찾게 escape ('!' 는 UserRepository.searchForAdmin 의 escape 문자) */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
