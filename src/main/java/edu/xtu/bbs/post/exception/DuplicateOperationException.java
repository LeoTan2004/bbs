package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Exception thrown when a duplicate operation is attempted
 * (e.g., liking an already liked post)
 */
public class DuplicateOperationException extends BusinessException {
    
    public DuplicateOperationException(String message) {
        super(ResponseCode.POST_ALREADY_LIKED, message);
    }
    
    public DuplicateOperationException(String message, Throwable cause) {
        super(ResponseCode.POST_ALREADY_LIKED, message, cause);
    }
}