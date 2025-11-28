package edu.xtu.bbs.post.exception;

import lombok.Getter;

@Getter
public class SensitiveContentException extends RuntimeException {
    private final String contentType;
    private final String detectedReason;
    private final String contentPreview;

    public SensitiveContentException(String contentType, String detectedReason) {
        super("Sensitive content detected in " + contentType + ": " + detectedReason);
        this.contentType = contentType;
        this.detectedReason = detectedReason;
        this.contentPreview = null;
    }

    public SensitiveContentException(String contentType, String detectedReason, String contentPreview) {
        super("Sensitive content detected in " + contentType + ": " + detectedReason);
        this.contentType = contentType;
        this.detectedReason = detectedReason;
        this.contentPreview = contentPreview != null && contentPreview.length() > 100
                ? contentPreview.substring(0, 100) + "..."
                : contentPreview;
    }

    public SensitiveContentException(String contentType, String detectedReason, String contentPreview, String customMessage) {
        super("Sensitive content detected in " + contentType + ": " + customMessage);
        this.contentType = contentType;
        this.detectedReason = detectedReason;
        this.contentPreview = contentPreview != null && contentPreview.length() > 100
                ? contentPreview.substring(0, 100) + "..."
                : contentPreview;
    }
}