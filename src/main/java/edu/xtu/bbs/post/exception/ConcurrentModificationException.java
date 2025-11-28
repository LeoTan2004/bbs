package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Exception thrown when concurrent modification is detected
 */
public class ConcurrentModificationException extends BusinessException {
    
    public ConcurrentModificationException(String message) {
        super(ResponseCode.SYSTEM_ERROR, message);
    }
    
    public ConcurrentModificationException(String message, Throwable cause) {
        super(ResponseCode.SYSTEM_ERROR, message, cause);
    }
}