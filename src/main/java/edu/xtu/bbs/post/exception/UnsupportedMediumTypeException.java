package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class UnsupportedMediumTypeException extends Exception {
    private final String fileName;
    private final String fileType;
    private final String[] supportedTypes;

    public UnsupportedMediumTypeException(String fileName, String fileType, String[] supportedTypes) {
        super("Unsupported medium type '" + fileType + "' for file '" + fileName + "'. Supported types: " + String.join(", ", supportedTypes));
        this.fileName = fileName;
        this.fileType = fileType;
        this.supportedTypes = supportedTypes;
    }

    public UnsupportedMediumTypeException(String fileName, String fileType, String[] supportedTypes, String message) {
        super("Unsupported medium type '" + fileType + "' for file '" + fileName + "': " + message);
        this.fileName = fileName;
        this.fileType = fileType;
        this.supportedTypes = supportedTypes;
    }
}