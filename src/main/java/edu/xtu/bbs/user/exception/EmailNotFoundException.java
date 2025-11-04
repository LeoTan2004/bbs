package edu.xtu.bbs.user.exception;

public class EmailNotFoundException extends Exception {
    private final String email;


    public EmailNotFoundException(String email, String message) {
        super(message);
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailNotFoundException{" +
                "email='" + email + '\'' +
                '}';
    }
}
