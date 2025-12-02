package edu.xtu.bbs.post.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

/**
 * Post media file upload configuration class
 * Configures parameters for post media file uploads, including storage bucket, file type restrictions, etc.
 * 
 * @author BBS Team
 */
@ConfigurationProperties(prefix = "bbs.oss.post-media")
@Component
@Data
@Validated
public class MediaConfiguration {

    /**
     * OSS bucket name for media storage
     */
    @NotBlank
    private String bucket;

    /**
     * Storage path prefix for media files
     */
    private String prefix = "/posts";

    /**
     * Maximum size for individual media files, defaults to 50MB
     */
    private DataSize maxSize = DataSize.ofMegabytes(50);

    /**
     * Maximum number of media files per post
     */
    @Positive
    private int maxFiles = 10;

    /**
     * List of allowed MIME types for media file uploads
     */
    private List<String> allowTypes = List.of(
            // Image types
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            // Video types  
            "video/mp4", "video/mpeg", "video/quicktime", "video/webm",
            // Audio types
            "audio/mpeg", "audio/wav", "audio/ogg",
            // Document types
            "application/pdf", "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Expiration time for signed URLs, defaults to 60 seconds
     */
    private Duration expiredAfter = Duration.ofSeconds(60);
    
    /**
     * Check if the media type is allowed for posts
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