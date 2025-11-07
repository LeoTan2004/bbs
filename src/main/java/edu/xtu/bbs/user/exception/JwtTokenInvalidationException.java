package edu.xtu.bbs.user.exception;

public class JwtTokenInvalidationException extends Exception {
    public JwtTokenInvalidationException(String message) {
        super(message);
    }
}
