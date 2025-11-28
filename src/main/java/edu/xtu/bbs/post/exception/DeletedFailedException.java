package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class DeletedFailedException extends RuntimeException {

    private final int postId;

    public DeletedFailedException(int postId, String message) {
        super("Failed to delete post with ID " + postId + ": " + message);
        this.postId = postId;
    }

}
