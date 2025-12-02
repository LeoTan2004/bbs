package edu.xtu.bbs.notification.dto;

import java.io.Serializable;

/**
 * Lightweight user representation for embedding inside notification responses.
 */
public record NotificationUserSummary(
        Integer id,
        String username,
        String nickname,
        String avatarUrl
) implements Serializable {
}
