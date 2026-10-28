package com.taxipot.domain.user.service;

import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 담당: 회원·운영 — 스케줄러 작업 5·6 */
@Service
@RequiredArgsConstructor
public class MemberJobService {

    /** 5. 정지 만료 해제 (1분): suspended_until &lt;= now 인 SUSPENDED → ACTIVE (User.releaseSuspension). NULL(영구)은 제외 */
    public void releaseExpiredSuspensions() {
        throw new NotImplementedYetException("스케줄러 5 정지 만료 해제");
    }

    /**
     * 6. 탈퇴 회원 개인정보 파기 (일 1회): withdrawn_at + 14일 &lt;= now → User.purgePersonalInfo(),
     * WITHDRAWAL 계좌 스냅샷 파기(Withdrawal.purgeAccount), STUDENT_VERIFICATION 행 DELETE + 캡처 파일 삭제(FileStorage.delete)
     */
    public void purgeWithdrawnMembers() {
        throw new NotImplementedYetException("스케줄러 6 탈퇴 회원 개인정보 파기");
    }
}
