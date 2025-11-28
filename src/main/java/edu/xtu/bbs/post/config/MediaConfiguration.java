package edu.xtu.bbs.post.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

/**
 * Media file upload configuration class
 * Configures parameters for media file uploads, including storage bucket, file type restrictions, etc.
 * 
 * @author BBS Team
 */
@ConfigurationProperties(prefix = "bbs.oss.media")
@Component
@Data
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
}