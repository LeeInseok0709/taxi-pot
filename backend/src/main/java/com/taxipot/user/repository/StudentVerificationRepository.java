package com.taxipot.user.repository;

import com.taxipot.user.entity.StudentVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** 조회 메서드 추가는 user 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {

    /** 가장 최근 인증 행 (NOT_VERIFIED detail, 재제출 판단) */
    Optional<StudentVerification> findFirstByMemberIdOrderByIdDesc(Long memberId);
}
