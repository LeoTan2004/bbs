package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class TooManyMediaException extends Exception {
    private final Integer postId;
    private final Integer currentMediaCount;
    private final Integer maxAllowedCount;
    private final Integer attemptedAddCount;

    public TooManyMediaException(Integer postId, Integer currentMediaCount, Integer maxAllowedCount) {
        super("Post " + postId + " already has " + currentMediaCount + " media files. Maximum allowed: " + maxAllowedCount);
        this.postId = postId;
        this.currentMediaCount = currentMediaCount;
        this.maxAllowedCount = maxAllowedCount;
        this.attemptedAddCount = 1;
    }

    public TooManyMediaException(Integer postId, Integer currentMediaCount, Integer maxAllowedCount, Integer attemptedAddCount) {
        super("Post " + postId + " has " + currentMediaCount + " media files. Adding " + attemptedAddCount + " more would exceed the maximum limit of " + maxAllowedCount);
        this.postId = postId;
        this.currentMediaCount = currentMediaCount;
        this.maxAllowedCount = maxAllowedCount;
        this.attemptedAddCount = attemptedAddCount;
    }

    public TooManyMediaException(Integer postId, Integer currentMediaCount, Integer maxAllowedCount, Integer attemptedAddCount, String message) {
        super("Too many media files for post " + postId + ": " + message);
        this.postId = postId;
        this.currentMediaCount = currentMediaCount;
        this.maxAllowedCount = maxAllowedCount;
        this.attemptedAddCount = attemptedAddCount;
    }
}