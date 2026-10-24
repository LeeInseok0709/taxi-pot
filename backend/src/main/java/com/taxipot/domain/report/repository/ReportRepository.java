package com.taxipot.domain.report.repository;

import com.taxipot.domain.report.entity.Report;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAllByReporterId(Long reporterId, Pageable pageable);

    Page<Report> findAllByReportedId(Long reportedId, Pageable pageable);

    Page<Report> findAllByStatusAndType(ReportStatus status, ReportType type, Pageable pageable);

    boolean existsByPotIdAndReporterIdAndReportedId(Long potId, Long reporterId, Long reportedId);
}
