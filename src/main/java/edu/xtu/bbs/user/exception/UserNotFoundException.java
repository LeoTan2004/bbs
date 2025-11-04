package edu.xtu.bbs.user.exception;

public class UserNotFoundException extends NotFoundException {
    private static final String DEFAULT_MSG = "";
    private final String principle;
    private final String msg;

    public UserNotFoundException(String principle) {
        this(principle, DEFAULT_MSG);
    }

    public UserNotFoundException(String principle, String msg) {
        this.principle = principle;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "UserNotFoundException: [%s], %s".formatted(this.principle, this.msg);
    }
}
