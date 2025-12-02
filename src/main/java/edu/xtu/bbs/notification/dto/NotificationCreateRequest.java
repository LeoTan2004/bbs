package edu.xtu.bbs.notification.dto;

import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Map;

/**
 * Request payload for creating a notification.
 */
public record NotificationCreateRequest(
        @NotNull Integer targetUserId,
        Integer actorUserId,
        @NotNull NotificationType type,
        NotificationPriority priority,
        @Size(max = 255) String title,
        String content,
        @Size(max = 1024) String redirectUrl,
        @Size(max = 100) String sourceType,
        @Size(max = 100) String sourceId,
        @Size(max = 512) String sourceSnippet,
        Map<String, Object> context
) implements Serializable {
}
