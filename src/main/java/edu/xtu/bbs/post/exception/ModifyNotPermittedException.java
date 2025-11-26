package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class ModifyNotPermittedException extends Exception {
    private final int postId;
    private final int userId;

    public ModifyNotPermittedException(int postId, int userId, String message) {
        super("Post " + postId + "is not allow to be modified by user " + userId + ": " + message);
        this.postId = postId;
        this.userId = userId;
    }
}
