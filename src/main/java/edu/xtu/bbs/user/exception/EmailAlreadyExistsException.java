package edu.xtu.bbs.user.exception;

public class EmailAlreadyExistsException extends Exception {
    private final String email;

    public EmailAlreadyExistsException(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailAlreadyExistsException: " + email + " already exists";
    }
}