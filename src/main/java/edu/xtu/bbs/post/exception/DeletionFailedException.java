package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;
import lombok.Getter;

/**
 * Exception thrown when deletion operation fails
 */
@Getter
public class DeletionFailedException extends BusinessException {

    private final Integer resourceId;
    private final String resourceType;

    public DeletionFailedException(Integer resourceId, String resourceType, String message) {
        super(ResponseCode.SYSTEM_ERROR, "Failed to delete " + resourceType + " with ID " + resourceId + ": " + message);
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public DeletionFailedException(Integer resourceId, String resourceType, String message, Throwable cause) {
        super(ResponseCode.SYSTEM_ERROR, "Failed to delete " + resourceType + " with ID " + resourceId + ": " + message, cause);
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
}