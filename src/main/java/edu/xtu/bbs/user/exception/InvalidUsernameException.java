package edu.xtu.bbs.user.exception;

public class InvalidUsernameException extends Exception {

    protected final String username;

    public InvalidUsernameException(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "InvalidUsernameException: " + username;
    }
}
