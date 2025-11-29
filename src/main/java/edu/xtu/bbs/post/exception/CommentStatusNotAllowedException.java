package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.post.model.CommentStatus;

/**
 * Exception thrown when a comment operation is not allowed due to its current status
 */
public class CommentStatusNotAllowedException extends Exception {

    private final Integer commentId;
    private final CommentStatus currentStatus;
    private final String operation;

    public CommentStatusNotAllowedException(Integer commentId, CommentStatus currentStatus, String operation) {
        super(String.format("Comment %d with status %s does not allow operation: %s", 
            commentId, currentStatus, operation));
        this.commentId = commentId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public CommentStatusNotAllowedException(Integer commentId, CommentStatus currentStatus, String operation, String message) {
        super(message);
        this.commentId = commentId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public CommentStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getOperation() {
        return operation;
    }
}