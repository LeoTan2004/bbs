package edu.xtu.bbs.common.exception;

import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Thrown when user submitted content fails {@code @ContentAudit} validation.
 */
public class ContentAuditViolationException extends BusinessException {

    public ContentAuditViolationException(String fieldPath, String message) {
        super(ResponseCode.PARAM_INVALID,
                message != null ? message : defaultMessage(fieldPath));
    }

    private static String defaultMessage(String fieldPath) {
        if (fieldPath == null || fieldPath.isBlank()) {
            return "Content contains prohibited expressions.";
        }
        return "Field '%s' contains prohibited expressions.".formatted(fieldPath);
    }
}
