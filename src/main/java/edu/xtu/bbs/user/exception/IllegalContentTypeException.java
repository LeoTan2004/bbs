package edu.xtu.bbs.user.exception;

public class IllegalContentTypeException extends Exception {
    public final String targetContentType;

    public IllegalContentTypeException(String targetContentType, String message) {
        super(message);
        this.targetContentType = targetContentType;
    }

    @Override
    public String toString() {
        return "IllegalContentTypeException{" +
                "targetContentType='" + targetContentType + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
