package edu.xtu.bbs.post.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

/**
 * Comment media file upload configuration class
 * Configures parameters for comment media file uploads, including storage bucket, file type restrictions, etc.
 * 
 * @author BBS Team
 */
@ConfigurationProperties(prefix = "bbs.oss.comment-media")
@Component
@Data
public class CommentMediaConfiguration {

    /**
     * OSS bucket name for comment media storage
     */
    @NotBlank
    private String bucket;

    /**
     * Storage path prefix for comment media files
     */
    private String prefix = "/comments";

    /**
     * Maximum size for individual comment media files, defaults to 20MB
     */
    private DataSize maxSize = DataSize.ofMegabytes(20);

    /**
     * Maximum number of media files per comment
     */
    @Positive
    private int maxFiles = 5;

    /**
     * List of allowed MIME types for comment media file uploads (more restrictive than posts)
     */
    private List<String> allowTypes = List.of(
            // Image types (more restrictive than posts)
            "image/jpeg", "image/png", "image/gif", "image/webp",
            // Video types (more restrictive than posts)
            "video/mp4", "video/webm",
            // Audio types
            "audio/mpeg", "audio/wav",
            // Basic document types
            "text/plain", "application/pdf"
    );

    /**
     * Expiration time for signed URLs, defaults to 60 seconds
     */
    private Duration expiredAfter = Duration.ofSeconds(60);
    
    /**
     * Check if the media type is allowed for comments
     *
     * @param mediaType the media type to check
     * @return true if allowed, false otherwise
     */
    public boolean isTypeAllowed(String mediaType) {
        return allowTypes.contains(mediaType);
    }

    /**
     * Get max size in bytes
     *
     * @return max size in bytes
     */
    public long getMaxSizeBytes() {
        return maxSize.toBytes();
    }
}