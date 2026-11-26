package com.taxipot.domain.user.mapper;

import com.taxipot.domain.user.dto.MyProfileResponse;
import com.taxipot.domain.user.entity.User;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.time.Kst;

/** User → 응답 DTO 변환 (여러 API 에서 공용) */
public final class UserMapper {

    /** 신고 누적 정지 기준: 3회 3일 / 6회 7일 / 10회 영구 */
    private static final int[] SUSPENSION_THRESHOLDS = {3, 6, 10};

    private UserMapper() {
    }

    public static MyProfileResponse toMyProfile(User u) {
        return new MyProfileResponse(
                u.getId(), u.getEmail(), u.getName(), u.getNickname(), u.getPhone(), u.getGender(),
                u.getUniversity(), u.getStudentNo(), u.getIsVerified(), u.getRatingAvg().doubleValue(),
                u.getNoShowCount(), u.getReportCount(), u.getCleanStreak(), u.getRole(), u.getStatus(),
                Kst.toOffset(u.getSuspendedUntil()), u.getAgreeMarketing(),
                Kst.toOffset(u.getCreatedAt()), Kst.toOffset(u.getUpdatedAt()), nextSuspensionIn(u));
    }

    /** 다음 정지까지 남은 신고 횟수 (영구 정지면 null) */
    public static Integer nextSuspensionIn(User u) {
        if (u.getStatus() == UserStatus.SUSPENDED && u.getSuspendedUntil() == null) {
            return null;
        }
        for (int threshold : SUSPENSION_THRESHOLDS) {
            if (u.getReportCount() < threshold) {
                return threshold - u.getReportCount();
            }
        }
        return null;
    }
}
