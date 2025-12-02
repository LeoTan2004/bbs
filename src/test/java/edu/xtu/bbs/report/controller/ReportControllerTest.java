package edu.xtu.bbs.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.exception.GlobalExceptionHandler;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.common.validation.SensitiveWordsDetector;
import edu.xtu.bbs.report.dto.CreateReportRequest;
import edu.xtu.bbs.report.dto.ReportResponse;
import edu.xtu.bbs.report.dto.ReportResultRequest;
import edu.xtu.bbs.report.dto.ReportTargetInfo;
import edu.xtu.bbs.report.exception.ReportAlreadyProcessedException;
import edu.xtu.bbs.report.exception.ReportNotFoundException;
import edu.xtu.bbs.report.model.ReportStatus;
import edu.xtu.bbs.report.model.ReportTargetType;
import edu.xtu.bbs.report.service.ReportService;
import edu.xtu.bbs.user.config.SecurityConfig;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ReportController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}, excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class), @ComponentScan.Filter(type = FilterType.REGEX, pattern = "edu\\.xtu\\.bbs\\.user\\.filter\\..*")})
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private SensitiveWordsDetector sensitiveWordsDetector;

    private User reporter;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(101);
        reporter.setRole(Role.User);
        reporter.setUsername("reporter");
    }

    @Test
    @DisplayName("POST /reports returns created report when authenticated")
    void createReport_shouldReturnReportForAuthenticatedUser() throws Exception {
        CreateReportRequest request = new CreateReportRequest(ReportTargetType.POST, 88, "垃圾广告", "反复刷屏");
        ReportResponse response = sampleResponse(1L, ReportStatus.PENDING);

        when(authenticationService.getCurrentUser()).thenReturn(reporter);
        when(reportService.createReport(eq(reporter), eq(request))).thenReturn(response);

        performPost(request).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.data.id").value(1L)).andExpect(jsonPath("$.data.status").value("PENDING")).andExpect(jsonPath("$.data.target.type").value("POST"));

        verify(reportService).createReport(reporter, request);
    }

    @Test
    @DisplayName("POST /reports returns 403 when unauthenticated")
    void createReport_shouldReturnForbiddenWhenUnauthenticated() throws Exception {
        CreateReportRequest request = new CreateReportRequest(ReportTargetType.POST, 88, "垃圾广告", "反复刷屏");
        when(authenticationService.getCurrentUser()).thenReturn(null);

        performPost(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ResponseCode.FORBIDDEN.getCode())).andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("POST /reports returns validation error when service throws business exception")
    void createReport_shouldPropagateBusinessError() throws Exception {
        CreateReportRequest request = new CreateReportRequest(ReportTargetType.USER, 999, "恶意骚扰", null);

        when(authenticationService.getCurrentUser()).thenReturn(reporter);
        when(reportService.createReport(eq(reporter), eq(request))).thenThrow(new BusinessException(ResponseCode.REPORT_TARGET_NOT_FOUND, "Reported target not found"));

        performPost(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(ResponseCode.REPORT_TARGET_NOT_FOUND.getCode())).andExpect(jsonPath("$.message").value("Reported target not found"));
    }

    @Test
    @DisplayName("GET /reports returns paged results for authenticated user")
    void getReports_shouldReturnPagedResult() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 10);
        ReportResponse response = sampleResponse(5L, ReportStatus.PENDING);
        Page<ReportResponse> page = new PageImpl<>(List.of(response), pageRequest, 1);

        when(authenticationService.getCurrentUser()).thenReturn(reporter);
        when(reportService.getReports(eq(reporter), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/reports").param("page", "0").param("size", "10")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(5L)).andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        verify(reportService).getReports(eq(reporter), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /reports returns 403 when unauthenticated")
    void getReports_shouldReturnForbiddenWhenUnauthenticated() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(null);

        mockMvc.perform(get("/reports")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ResponseCode.FORBIDDEN.getCode())).andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("GET /reports/{id} returns report when authenticated")
    void getReport_shouldReturnReport() throws Exception {
        ReportResponse response = sampleResponse(77L, ReportStatus.PENDING);
        when(authenticationService.getCurrentUser()).thenReturn(reporter);
        when(reportService.getReport(reporter, 77L)).thenReturn(response);

        mockMvc.perform(get("/reports/{reportId}", 77L)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(77L));

        verify(reportService).getReport(reporter, 77L);
    }

    @Test
    @DisplayName("GET /reports/{id} returns 403 when unauthenticated")
    void getReport_shouldReturnForbiddenWhenUnauthenticated() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(null);

        mockMvc.perform(get("/reports/{reportId}", 99L)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ResponseCode.FORBIDDEN.getCode())).andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("GET /reports/{id} returns 200 business error when report missing")
    void getReport_shouldReturnNotFound() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(reporter);
        when(reportService.getReport(reporter, 999L)).thenThrow(new ReportNotFoundException(999L));

        mockMvc.perform(get("/reports/{reportId}", 999L)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(ResponseCode.REPORT_NOT_FOUND.getCode())).andExpect(jsonPath("$.message").value("Report not found: 999"));
    }

    private ResultActions performPost(Object payload) throws Exception {
        return mockMvc.perform(post("/reports").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(payload))).andDo(print());
    }

    private ResultActions performPatch(Long id, Object payload) throws Exception {
        return mockMvc.perform(patch("/reports/{id}/result", id).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(payload))).andDo(print());
    }

    private ReportResponse sampleResponse(Long id, ReportStatus status) {
        ReportTargetInfo target = new ReportTargetInfo(ReportTargetType.POST, 88, "考试心得分享", "违规摘要");
        Instant now = Instant.parse("2025-12-02T12:00:00Z");
        return new ReportResponse(id, status, target, "垃圾广告", "反复刷屏", status == ReportStatus.RESOLVED ? "违规成立" : null, status == ReportStatus.RESOLVED, status == ReportStatus.RESOLVED ? now : null, now, now);
    }

    @Nested
    @DisplayName("PATCH /reports/{id}/result")
    class PublishResult {

        private User admin;

        @BeforeEach
        void initAdmin() {
            admin = new User();
            admin.setId(1);
            admin.setRole(Role.Admin);
            admin.setUsername("admin");
        }

        @Test
        @DisplayName("returns updated report when admin publishes result")
        void publishResult_shouldReturnUpdatedReport() throws Exception {
            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "违规成立", "/reports/1");
            ReportResponse response = sampleResponse(1L, ReportStatus.RESOLVED);

            when(authenticationService.getCurrentUser()).thenReturn(admin);
            when(reportService.publishResult(admin, 1L, request)).thenReturn(response);

            performPatch(1L, request).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RESOLVED")).andExpect(jsonPath("$.data.resultNotified").value(true));

            verify(reportService).publishResult(admin, 1L, request);
        }

        @Test
        @DisplayName("returns 403 when operator unauthenticated")
        void publishResult_shouldReturnForbiddenWhenUnauthenticated() throws Exception {
            ReportResultRequest request = new ReportResultRequest(ReportStatus.REJECTED, "未发现违规", null);
            when(authenticationService.getCurrentUser()).thenReturn(null);

            performPatch(2L, request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ResponseCode.FORBIDDEN.getCode())).andExpect(jsonPath("$.message").value("Authentication required"));

            verifyNoInteractions(reportService);
        }

        @Test
        @DisplayName("returns business error when admin lacks permission")
        void publishResult_shouldReturnBusinessError() throws Exception {
            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "ok", null);
            when(authenticationService.getCurrentUser()).thenReturn(admin);
            when(reportService.publishResult(admin, 3L, request)).thenThrow(new BusinessException(ResponseCode.FORBIDDEN, "Only administrators can publish report results"));

            performPatch(3L, request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(ResponseCode.FORBIDDEN.getCode())).andExpect(jsonPath("$.message").value("Only administrators can publish report results"));
        }

        @Test
        @DisplayName("returns business error when report already processed")
        void publishResult_shouldReturnAlreadyProcessed() throws Exception {
            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "done", null);
            when(authenticationService.getCurrentUser()).thenReturn(admin);
            when(reportService.publishResult(admin, 4L, request)).thenThrow(new ReportAlreadyProcessedException(4L));

            performPatch(4L, request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(ResponseCode.REPORT_ALREADY_RESOLVED.getCode()));
        }
    }
}
