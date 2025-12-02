package edu.xtu.bbs.notification.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Thrown when a notification cannot be located for the given user.
 */
public class NotificationNotFoundException extends BusinessException {

    public NotificationNotFoundException(Long notificationId) {
        super(ResponseCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + notificationId);
    }

    public NotificationNotFoundException(String message) {
        super(ResponseCode.NOTIFICATION_NOT_FOUND, message);
    }
}
