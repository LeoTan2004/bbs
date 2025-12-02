package edu.xtu.bbs.notification.service;

import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse create(NotificationCreateRequest request);

    Page<NotificationResponse> getNotifications(Integer userId, Pageable pageable);

    NotificationResponse markAsRead(Integer userId, Long notificationId);

    int markAllAsRead(Integer userId);

    long countUnread(Integer userId);
}
