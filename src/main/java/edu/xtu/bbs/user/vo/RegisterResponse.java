package edu.xtu.bbs.user.vo;

/**
 * Registration Response VO
 * Used for returning registration result
 */
public record RegisterResponse(String email, String message) {

    public RegisterResponse(String email) {
        this(email, "Registration successful");
    }
}