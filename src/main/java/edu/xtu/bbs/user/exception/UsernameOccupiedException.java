package edu.xtu.bbs.user.exception;

public class UsernameOccupiedException extends InvalidUsernameException {
    public UsernameOccupiedException(String username) {
        super(username);
    }

    @Override
    public String toString() {
        return "UsernameOccupiedException: " + this.username;
    }
}
