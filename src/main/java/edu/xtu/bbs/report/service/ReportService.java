package edu.xtu.bbs.report.service;

import edu.xtu.bbs.report.dto.CreateReportRequest;
import edu.xtu.bbs.report.dto.ReportResponse;
import edu.xtu.bbs.report.dto.ReportResultRequest;
import edu.xtu.bbs.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponse createReport(User reporter, CreateReportRequest request);

    Page<ReportResponse> getReports(User reporter, Pageable pageable);

    ReportResponse getReport(User reporter, Long reportId);

    ReportResponse publishResult(User operator, Long reportId, ReportResultRequest request);
}
