package com.taxipot.domain.verification.repository;

import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {

    /** 가장 최근 인증 행 */
    Optional<StudentVerification> findFirstByMemberIdOrderByIdDesc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentVerification> findFirstByMemberIdAndEmailStatusOrderByIdDesc(Long memberId, EmailStatus emailStatus);

    boolean existsByMemberIdAndEmailStatus(Long memberId, EmailStatus emailStatus);

    List<StudentVerification> findAllByMemberId(Long memberId);

    Page<StudentVerification> findAllByReviewStatus(ReviewStatus reviewStatus, Pageable pageable);

    /**
     * 실패 횟수 + 1 (명세: 동시 입력으로 6 이상이 되지 않게 조건부 UPDATE).
     * @return 1 이면 증가됨, 0 이면 이미 5회
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudentVerification v set v.emailCodeAttempts = v.emailCodeAttempts + 1 "
            + "where v.id = :id and v.emailCodeAttempts < :max")
    int increaseAttempts(@Param("id") Long id, @Param("max") int max);
}
