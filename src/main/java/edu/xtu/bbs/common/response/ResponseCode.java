package edu.xtu.bbs.common.response;

import lombok.Getter;
import lombok.ToString;

/**
 * Unified Response Code Enumeration
 * Adopts 5-digit number encoding standard:
 * - General module: 1xxxx
 * - User module: 2xxxx
 * - Post module: 3xxxx
 * - Comment module: 4xxxx
 * - Notification module: 5xxxx
 * - System module: 9xxxx
 */
@Getter
@ToString
public enum ResponseCode {

    // ==================== General Module (1xxxx) ====================
    SUCCESS(10000, "Success"),
    PARAM_INVALID(10001, "Invalid parameters"),
    UNAUTHORIZED(10002, "Not logged in or token expired"),
    FORBIDDEN(10003, "Insufficient permissions"),
    TOO_MANY_REQUESTS(10004, "Too many requests"),

    // ==================== User Module (2xxxx) ====================
    USER_NOT_FOUND(20001, "User not found"),
    PASSWORD_INCORRECT(20002, "Incorrect password"),
    USER_ALREADY_EXISTS(20003, "User already exists"),
    FOLLOW_SELF_NOT_ALLOWED(20004, "Cannot follow yourself"),

    // ==================== Post Module (3xxxx) ====================
    POST_NOT_FOUND(30001, "Post not found"),
    POST_PERMISSION_DENIED(30002, "No permission to operate post"),
    POST_CONTENT_EMPTY(30003, "Post content is empty"),
    POST_ALREADY_LIKED(30004, "Already liked"),
    POST_REPORT_SUCCESS(30005, "Report submitted successfully"),

    // ==================== Comment Module (4xxxx) ====================
    COMMENT_NOT_FOUND(40001, "Comment not found"),
    COMMENT_CONTENT_EMPTY(40002, "Comment content is empty"),
    COMMENT_PERMISSION_DENIED(40003, "No permission to delete comment"),

    // ==================== Notification Module (5xxxx) ====================
    NOTIFICATION_NOT_FOUND(50001, "Notification not found"),
    MESSAGE_SEND_FAIL(50002, "Message send failed"),

    // ==================== System Module (9xxxx) ====================
    SYSTEM_ERROR(90001, "Internal server error"),
    SERVICE_UNAVAILABLE(90002, "Service unavailable"),
    DB_ERROR(90003, "Database operation failed"),
    FILE_UPLOAD_ERROR(90004, "File upload failed");

    private final Integer code;
    private final String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}