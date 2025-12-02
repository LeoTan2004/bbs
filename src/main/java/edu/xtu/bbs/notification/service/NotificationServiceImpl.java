package edu.xtu.bbs.notification.service;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.dto.NotificationResponse;
import edu.xtu.bbs.notification.dto.NotificationUserSummary;
import edu.xtu.bbs.notification.exception.NotificationNotFoundException;
import edu.xtu.bbs.notification.model.Notification;
import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.repo.NotificationRepository;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationResponse create(NotificationCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Invalid notification creation request");
        }

        final User targetUser = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND,
                        "Target user not found: " + request.targetUserId()));

        User actorUser = null;
        if (request.actorUserId() != null) {
            actorUser = userRepository.findById(request.actorUserId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND,
                            "Actor user not found: " + request.actorUserId()));
        }

        Notification notification = new Notification();
        notification.setTargetUser(targetUser);
        notification.setActor(actorUser);
        notification.setType(request.type());
        notification.setPriority(request.priority() != null ? request.priority() : NotificationPriority.NORMAL);
        notification.setTitle(request.title());
        notification.setContent(request.content());
        notification.setRedirectUrl(request.redirectUrl());
        notification.setSourceType(request.sourceType());
        notification.setSourceId(request.sourceId());
        notification.setSourceSnippet(request.sourceSnippet());
        notification.setContext(request.context() != null ? Map.copyOf(request.context()) : Map.of());

        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Integer userId, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        Page<Notification> notifications = notificationRepository.findByTargetUserId(userId, pageable);
        return notifications.map(this::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(Integer userId, Long notificationId) {
        if (userId == null || notificationId == null) {
            throw new IllegalArgumentException("User id and notification id are required");
        }

        Notification notification = notificationRepository.findByIdAndTargetUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(Boolean.TRUE);
            notification.setReadAt(java.time.Instant.now());
        }

        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public int markAllAsRead(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }

        var unreadNotifications = notificationRepository.findByTargetUserIdAndReadFalse(userId);
        if (unreadNotifications.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        unreadNotifications.forEach(notification -> {
            notification.setRead(Boolean.TRUE);
            notification.setReadAt(now);
        });

        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return notificationRepository.countByTargetUserIdAndReadFalse(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationUserSummary actorSummary = null;
        if (notification.getActor() != null) {
            User actor = notification.getActor();
            actorSummary = new NotificationUserSummary(
                    actor.getId(),
                    actor.getUsername(),
                    actor.getNickname(),
                    actor.getAvatarUrl()
            );
        }

        Map<String, Object> context = notification.getContext() != null ? notification.getContext() : Map.of();

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getPriority(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRedirectUrl(),
                notification.getSourceType(),
                notification.getSourceId(),
                notification.getSourceSnippet(),
                context,
                Boolean.TRUE.equals(notification.getRead()),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getTargetUser() != null ? notification.getTargetUser().getId() : null,
                actorSummary
        );
    }
}
