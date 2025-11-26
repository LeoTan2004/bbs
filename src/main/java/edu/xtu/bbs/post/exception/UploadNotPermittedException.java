package edu.xtu.bbs.post.exception;

public class UploadNotPermittedException extends ModifyNotPermittedException {
    public UploadNotPermittedException(int userId, int postId, String message) {
        super(userId, postId, message);
    }

}
