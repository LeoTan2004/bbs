package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class MediumSizeExceededException extends RuntimeException {
    private final String fileName;
    private final Long fileSize;
    private final Long maxAllowedSize;

    public MediumSizeExceededException(String fileName, Long fileSize, Long maxAllowedSize) {
        super("Medium file '" + fileName + "' size " + formatBytes(fileSize) + " exceeds maximum allowed size " + formatBytes(maxAllowedSize));
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.maxAllowedSize = maxAllowedSize;
    }

    public MediumSizeExceededException(String fileName, Long fileSize, Long maxAllowedSize, String message) {
        super("Medium file '" + fileName + "' size exceeds limit: " + message);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.maxAllowedSize = maxAllowedSize;
    }

    private static String formatBytes(Long bytes) {
        if (bytes == null) return "unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}