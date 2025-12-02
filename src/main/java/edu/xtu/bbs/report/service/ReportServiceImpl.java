package edu.xtu.bbs.report.service;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.common.validation.ContentAuditService;
import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
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
import edu.xtu.bbs.report.dto.ReportTargetInfo;
import edu.xtu.bbs.report.exception.InvalidReportTargetException;
import edu.xtu.bbs.report.exception.ReportAlreadyProcessedException;
import edu.xtu.bbs.report.exception.ReportNotFoundException;
import edu.xtu.bbs.report.model.Report;
import edu.xtu.bbs.report.model.ReportStatus;
import edu.xtu.bbs.report.model.ReportTargetType;
import edu.xtu.bbs.report.repo.ReportRepository;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final NotificationService notificationService;
    private final ContentAuditService contentAuditService;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             PostRepository postRepository,
                             PostCommentRepository postCommentRepository,
                             NotificationService notificationService,
                             ContentAuditService contentAuditService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        this.notificationService = notificationService;
        this.contentAuditService = contentAuditService;
    }

    @Override
    public ReportResponse createReport(User reporter, CreateReportRequest request) {
        requireAuthenticatedUser(reporter);
        if (request == null) {
            throw new IllegalArgumentException("Invalid report creation request");
        }

        contentAuditService.validate(request);

        ensureTargetExists(request.targetType(), request.targetId());

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDescription(request.description());
        report.setStatus(ReportStatus.PENDING);
        report.setResult(null);
        report.setResultNotified(Boolean.FALSE);
        report.setResolvedAt(null);

        Report saved = reportRepository.save(report);
        Map<TargetKey, ReportTargetInfo> targetInfo = resolveTargetInfo(List.of(saved));
        return toResponse(saved, targetInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(User reporter, Pageable pageable) {
        requireAuthenticatedUser(reporter);
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must not be null");
        }

        Page<Report> reports = reportRepository.findByReporterId(reporter.getId(), pageable);
        Map<TargetKey, ReportTargetInfo> targetInfo = resolveTargetInfo(reports.getContent());
        return reports.map(report -> toResponse(report, targetInfo));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReport(User reporter, Long reportId) {
        requireAuthenticatedUser(reporter);
        if (reportId == null) {
            throw new IllegalArgumentException("Report id is required");
        }

        Report report = reportRepository.findByIdAndReporterId(reportId, reporter.getId())
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        Map<TargetKey, ReportTargetInfo> targetInfo = resolveTargetInfo(List.of(report));
        return toResponse(report, targetInfo);
    }

    @Override
    public ReportResponse publishResult(User operator, Long reportId, ReportResultRequest request) {
        requireAuthenticatedUser(operator);
        if (request == null) {
            throw new IllegalArgumentException("Invalid report result request");
        }
        if (reportId == null) {
            throw new IllegalArgumentException("Report id is required");
        }
        if (operator.getRole() != Role.Admin) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "Only administrators can publish report results");
        }

        contentAuditService.validate(request);

        if (request.status() == ReportStatus.PENDING) {
            throw new BusinessException(ResponseCode.PARAM_INVALID, "Report result status must not be PENDING");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ReportAlreadyProcessedException(reportId);
        }

        report.setStatus(request.status());
        report.setResult(request.result());
        report.setResolvedAt(Instant.now());
        report.setResultNotified(Boolean.TRUE);

        Report saved = reportRepository.save(report);

        Map<TargetKey, ReportTargetInfo> targetInfo = resolveTargetInfo(List.of(saved));
        ReportTargetInfo target = targetInfo.getOrDefault(new TargetKey(saved.getTargetType(), saved.getTargetId()),
                new ReportTargetInfo(saved.getTargetType(), saved.getTargetId(), null, null));

        sendResultNotification(operator, saved, target, request.redirectUrl());

        return toResponse(saved, targetInfo);
    }

    private void ensureTargetExists(ReportTargetType targetType, Integer targetId) {
        boolean exists;
        switch (targetType) {
            case USER -> exists = userRepository.existsById(targetId);
            case POST -> exists = postRepository.existsById(targetId);
            case COMMENT -> exists = postCommentRepository.existsById(targetId);
            default -> exists = false;
        }

        if (!exists) {
            throw new InvalidReportTargetException(targetType, targetId);
        }
    }

    private void requireAuthenticatedUser(User user) {
        if (user == null || user.getId() == null) {
            throw new SecurityException("Authentication required");
        }
    }

    private ReportResponse toResponse(Report report, Map<TargetKey, ReportTargetInfo> targetInfoMap) {
        ReportTargetInfo target = targetInfoMap.getOrDefault(new TargetKey(report.getTargetType(), report.getTargetId()),
                new ReportTargetInfo(report.getTargetType(), report.getTargetId(), null, null));

        return new ReportResponse(
                report.getId(),
                report.getStatus(),
                target,
                report.getReason(),
                report.getDescription(),
                report.getResult(),
                report.getResultNotified(),
                report.getResolvedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private Map<TargetKey, ReportTargetInfo> resolveTargetInfo(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<TargetKey, ReportTargetInfo> result = new HashMap<>();

        Set<Integer> userIds = new HashSet<>();
        Set<Integer> postIds = new HashSet<>();
        Set<Integer> commentIds = new HashSet<>();

        for (Report report : reports) {
            if (report == null || report.getTargetId() == null || report.getTargetType() == null) {
                continue;
            }
            switch (report.getTargetType()) {
                case USER -> userIds.add(report.getTargetId());
                case POST -> postIds.add(report.getTargetId());
                case COMMENT -> commentIds.add(report.getTargetId());
            }
        }

        if (!userIds.isEmpty()) {
            Map<Integer, User> users = iterableToStream(userRepository.findAllById(userIds))
                    .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
            users.forEach((id, user) -> result.put(new TargetKey(ReportTargetType.USER, id),
                    new ReportTargetInfo(ReportTargetType.USER, id,
                            user.getNickname() != null ? user.getNickname() : user.getUsername(),
                            null)));
        }

        if (!postIds.isEmpty()) {
            List<Post> posts = postRepository.findAllById(postIds);
            for (Post post : posts) {
                if (post.getId() == null) {
                    continue;
                }
                result.put(new TargetKey(ReportTargetType.POST, post.getId()),
                        new ReportTargetInfo(ReportTargetType.POST, post.getId(),
                                post.getTitle(), null));
            }
        }

        if (!commentIds.isEmpty()) {
            List<PostComment> comments = postCommentRepository.findAllById(commentIds);
            for (PostComment comment : comments) {
                if (comment.getId() == null) {
                    continue;
                }
                result.put(new TargetKey(ReportTargetType.COMMENT, comment.getId()),
                        new ReportTargetInfo(ReportTargetType.COMMENT, comment.getId(),
                                null, truncate(comment.getContent(), 120)));
            }
        }

        return result;
    }

    private void sendResultNotification(User operator, Report report, ReportTargetInfo targetInfo, String redirectUrl) {
        if (report.getReporter() == null || report.getReporter().getId() == null) {
            return;
        }

        String title = "举报处理结果通知";
        String content = buildResultContent(report, targetInfo);

        NotificationCreateRequest notificationRequest = new NotificationCreateRequest(
                report.getReporter().getId(),
                operator.getId(),
                NotificationType.REPORT_RESULT,
                NotificationPriority.NORMAL,
                title,
                content,
                redirectUrl != null ? redirectUrl : "/reports/" + report.getId(),
                report.getTargetType().name(),
                String.valueOf(report.getTargetId()),
                truncate(report.getReason(), 200),
                Map.of(
                        "reportId", report.getId(),
                        "status", report.getStatus().name(),
                        "targetType", report.getTargetType().name(),
                        "targetId", report.getTargetId()
                )
        );

        NotificationResponse response = notificationService.create(notificationRequest);
        if (response == null) {
            throw new BusinessException(ResponseCode.MESSAGE_SEND_FAIL, "Failed to send report result notification");
        }
    }

    private String buildResultContent(Report report, ReportTargetInfo targetInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append("您的举报已处理结果：");
        builder.append(report.getStatus() == ReportStatus.RESOLVED ? "举报成立" : "未通过");
        builder.append("。");

        if (targetInfo != null) {
            builder.append("举报对象：");
            builder.append(targetInfo.type().name());
            if (targetInfo.title() != null) {
                builder.append(" ").append(targetInfo.title());
            }
            builder.append(" (ID: ").append(targetInfo.id()).append(")。");
        }

        if (report.getResult() != null && !report.getResult().isBlank()) {
            builder.append("处理说明：").append(report.getResult());
        }

        return builder.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private <T> java.util.stream.Stream<T> iterableToStream(Iterable<T> iterable) {
        if (iterable == null) {
            return java.util.stream.Stream.empty();
        }
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }

    private record TargetKey(ReportTargetType type, Integer id) {
    }
}
