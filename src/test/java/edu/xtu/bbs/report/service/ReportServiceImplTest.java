package edu.xtu.bbs.report.service;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.notification.dto.NotificationResponse;
import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;
import edu.xtu.bbs.notification.service.NotificationService;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.report.dto.CreateReportRequest;
import edu.xtu.bbs.report.dto.ReportResponse;
import edu.xtu.bbs.report.dto.ReportResultRequest;
import edu.xtu.bbs.report.exception.ReportAlreadyProcessedException;
import edu.xtu.bbs.report.model.Report;
import edu.xtu.bbs.report.model.ReportStatus;
import edu.xtu.bbs.report.model.ReportTargetType;
import edu.xtu.bbs.report.repo.ReportRepository;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository postCommentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private edu.xtu.bbs.common.validation.ContentAuditService contentAuditService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User reporter;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(101);
        reporter.setRole(Role.User);
        reporter.setUsername("reporter");
    }

    @Test
    @DisplayName("createReport should persist report for post target")
    void createReportShouldPersist() {
        CreateReportRequest request = new CreateReportRequest(ReportTargetType.POST, 55, "垃圾广告", "重复刷屏");

        when(postRepository.existsById(55)).thenReturn(true);

        Report saved = new Report();
        saved.setId(200L);
        saved.setReporter(reporter);
        saved.setTargetType(ReportTargetType.POST);
        saved.setTargetId(55);
        saved.setReason("垃圾广告");
        saved.setStatus(ReportStatus.PENDING);

        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        Post post = new Post();
        post.setId(55);
        post.setTitle("考试心得分享");
        when(postRepository.findAllById(anyIterable())).thenReturn(List.of(post));

        ReportResponse response = reportService.createReport(reporter, request);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.target()).isNotNull();
        assertThat(response.target().type()).isEqualTo(ReportTargetType.POST);
        assertThat(response.target().title()).isEqualTo("考试心得分享");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report toSave = captor.getValue();
        assertThat(toSave.getReporter()).isEqualTo(reporter);
        assertThat(toSave.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(toSave.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("getReports should return paged results for reporter")
    void getReportsShouldReturnPage() {
        Report report = new Report();
        report.setId(300L);
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(402);
        report.setReason("恶意骚扰");
        report.setStatus(ReportStatus.PENDING);

        Page<Report> reportPage = new PageImpl<>(List.of(report), PageRequest.of(0, 10), 1);
        when(reportRepository.findByReporterId(eq(reporter.getId()), any(Pageable.class))).thenReturn(reportPage);

        User targetUser = new User();
        targetUser.setId(402);
        targetUser.setUsername("target");
        targetUser.setNickname("被举报者");
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(targetUser));

        Page<ReportResponse> responsePage = reportService.getReports(reporter, PageRequest.of(0, 10));

        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        ReportResponse response = responsePage.getContent().get(0);
        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.target()).isNotNull();
        assertThat(response.target().title()).isEqualTo("被举报者");
    }

    @Nested
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
        @DisplayName("publishResult should update report and send notification")
        void publishResultShouldUpdateReport() {
            Report report = new Report();
            report.setId(400L);
            report.setReporter(reporter);
            report.setTargetType(ReportTargetType.COMMENT);
            report.setTargetId(9001);
            report.setReason("辱骂");
            report.setStatus(ReportStatus.PENDING);

            when(reportRepository.findById(400L)).thenReturn(Optional.of(report));
            when(reportRepository.save(report)).thenReturn(report);

            PostComment comment = new PostComment();
            comment.setId(9001);
            comment.setContent("这是一条违规评论");
            when(postCommentRepository.findAllById(anyIterable())).thenReturn(List.of(comment));

            NotificationResponse notificationResponse = new NotificationResponse(
                    10L,
                    NotificationType.REPORT_RESULT,
                    NotificationPriority.NORMAL,
                    "举报处理结果通知",
                    "已处理",
                    "/reports/400",
                    ReportTargetType.COMMENT.name(),
                    "9001",
                    "辱骂",
                    Map.of(),
                    false,
                    null,
                    Instant.now(),
                    Instant.now(),
                    reporter.getId(),
                    null
            );
            when(notificationService.create(any())).thenReturn(notificationResponse);

            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "已处理完毕", null);

            ReportResponse response = reportService.publishResult(admin, 400L, request);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(report.getResult()).isEqualTo("已处理完毕");
            assertThat(report.getResultNotified()).isTrue();
            assertThat(report.getResolvedAt()).isNotNull();

            assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
            verify(notificationService).create(any());
        }

        @Test
        @DisplayName("publishResult should reject non-admin operator")
        void publishResultShouldRejectNonAdmin() {
            User normalUser = new User();
            normalUser.setId(2);
            normalUser.setRole(Role.User);

            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "ok", null);

            assertThatThrownBy(() -> reportService.publishResult(normalUser, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only administrators");
        }

        @Test
        @DisplayName("publishResult should reject already processed report")
        void publishResultShouldRejectProcessedReport() {
            Report report = new Report();
            report.setId(401L);
            report.setReporter(reporter);
            report.setTargetType(ReportTargetType.USER);
            report.setTargetId(7);
            report.setStatus(ReportStatus.RESOLVED);

            when(reportRepository.findById(401L)).thenReturn(Optional.of(report));

            ReportResultRequest request = new ReportResultRequest(ReportStatus.RESOLVED, "done", null);

            assertThatThrownBy(() -> reportService.publishResult(admin, 401L, request))
                    .isInstanceOf(ReportAlreadyProcessedException.class);
        }
    }
}
