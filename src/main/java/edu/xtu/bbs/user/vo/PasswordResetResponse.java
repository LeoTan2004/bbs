package edu.xtu.bbs.user.vo;

/**
 * Password Reset Response VO
 * Used for returning password reset operation result
 */
public record PasswordResetResponse(boolean success, String message) {


    public PasswordResetResponse(boolean success) {
        this(success, success ? "Password reset successful" : "Password reset failed");
    }
}