package edu.xtu.bbs.post.exception;

import edu.xtu.bbs.post.model.PostStatus;
import lombok.Getter;

@Getter
public class PostStatusNotAllowedException extends Exception {
    private final Integer postId;
    private final PostStatus currentStatus;
    private final String operation;

    public PostStatusNotAllowedException(Integer postId, PostStatus currentStatus, String operation) {
        super("Operation '" + operation + "' is not allowed for post " + postId + " with status " + currentStatus);
        this.postId = postId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public PostStatusNotAllowedException(Integer postId, PostStatus currentStatus, String operation, String message) {
        super("Operation '" + operation + "' is not allowed for post " + postId + " with status " + currentStatus + ": " + message);
        this.postId = postId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }
}