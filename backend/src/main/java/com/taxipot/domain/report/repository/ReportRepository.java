package com.taxipot.domain.report.repository;

import com.taxipot.domain.report.entity.Report;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAllByReporterId(Long reporterId, Pageable pageable);

    Page<Report> findAllByReportedId(Long reportedId, Pageable pageable);

    Page<Report> findAllByStatusAndType(ReportStatus status, ReportType type, Pageable pageable);

    boolean existsByPotIdAndReporterIdAndReportedId(Long potId, Long reporterId, Long reportedId);

    /**
     * 운영자 신고 목록: status·type 은 선택 필터 (null 이면 조건 없음). 정렬은 Pageable 로 넘긴다.
     * <p>인석 추가 — 위 findAllByStatusAndType 은 두 값이 모두 있어야 해서 '필터 없음'을 표현할 수 없다.
     */
    @Query("select r from Report r "
            + "where (:status is null or r.status = :status) "
            + "and (:type is null or r.type = :type)")
    Page<Report> search(@Param("status") ReportStatus status, @Param("type") ReportType type, Pageable pageable);

    /**
     * 신고 처리용 잠금 조회. 운영자 두 명이 같은 신고를 동시에 처리해 신고 횟수가 두 번 오르는 것을 막는다.
     * <p>잠금 순서: 피신고자 USERS 를 먼저 잠근 뒤 이 행을 잠근다 (CONVENTIONS 8장 'USERS 먼저').
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);
}
