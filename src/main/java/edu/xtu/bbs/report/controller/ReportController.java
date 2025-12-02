package edu.xtu.bbs.report.controller;

import edu.xtu.bbs.report.dto.CreateReportRequest;
import edu.xtu.bbs.report.dto.ReportResponse;
import edu.xtu.bbs.report.dto.ReportResultRequest;
import edu.xtu.bbs.report.service.ReportService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthenticationService authenticationService;

    public ReportController(ReportService reportService, AuthenticationService authenticationService) {
        this.reportService = reportService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ReportResponse createReport(@Valid @RequestBody CreateReportRequest request) {
        User reporter = requireCurrentUser();
        return reportService.createReport(reporter, request);
    }

    @GetMapping
    public Page<ReportResponse> getMyReports(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User reporter = requireCurrentUser();
        return reportService.getReports(reporter, pageable);
    }

    @GetMapping("/{reportId}")
    public ReportResponse getReport(@PathVariable Long reportId) {
        User reporter = requireCurrentUser();
        return reportService.getReport(reporter, reportId);
    }

    @PatchMapping("/{reportId}/result")
    public ReportResponse publishResult(@PathVariable Long reportId,
                                        @Valid @RequestBody ReportResultRequest request) {
        User operator = requireCurrentUser();
        return reportService.publishResult(operator, reportId, request);
    }

    private User requireCurrentUser() {
        User user = authenticationService.getCurrentUser();
        if (user == null) {
            throw new SecurityException("Authentication required");
        }
        return user;
    }
}
