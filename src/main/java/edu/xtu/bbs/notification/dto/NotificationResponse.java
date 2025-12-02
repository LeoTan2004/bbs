package edu.xtu.bbs.notification.dto;

import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Response payload returned to clients when fetching notifications.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        NotificationPriority priority,
        String title,
        String content,
        String redirectUrl,
        String sourceType,
        String sourceId,
        String sourceSnippet,
        Map<String, Object> context,
        boolean read,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt,
        Integer targetUserId,
        NotificationUserSummary actor
) implements Serializable {
}
