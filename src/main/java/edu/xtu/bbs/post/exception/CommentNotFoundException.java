package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Exception thrown when a comment is not found
 */
public class CommentNotFoundException extends BusinessException {
    
    public CommentNotFoundException(String message) {
        super(ResponseCode.COMMENT_NOT_FOUND, message);
    }
    
    public CommentNotFoundException(Integer commentId) {
        super(ResponseCode.COMMENT_NOT_FOUND, "Comment not found with id: " + commentId);
    }
}