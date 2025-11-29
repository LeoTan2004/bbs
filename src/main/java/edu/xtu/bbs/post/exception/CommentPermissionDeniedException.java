package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Exception thrown when user tries to perform unauthorized actions on comments
 */
public class CommentPermissionDeniedException extends BusinessException {
    
    public CommentPermissionDeniedException(String message) {
        super(ResponseCode.COMMENT_PERMISSION_DENIED, message);
    }
    
    public CommentPermissionDeniedException(Integer userId, Integer commentId, String action) {
        super(ResponseCode.COMMENT_PERMISSION_DENIED, String.format("User %d is not authorized to %s comment %d", userId, action, commentId));
    }
}