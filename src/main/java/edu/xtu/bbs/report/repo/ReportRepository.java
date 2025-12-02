package edu.xtu.bbs.report.repo;

import edu.xtu.bbs.report.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReporterId(Integer reporterId, Pageable pageable);

    Optional<Report> findByIdAndReporterId(Long id, Integer reporterId);
}
