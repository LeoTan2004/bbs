package edu.xtu.bbs.post.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Media service configuration class
 * Configures media service parameters such as base URL, file count limits, etc.
 * 
 * @author BBS Team
 */
@ConfigurationProperties(prefix = "bbs.media")
@Component
@Data
public class MediaServiceConfiguration {

    /**
     * Base URL for media service endpoints
     */
    @NotBlank
    private String baseUrl = "http://localhost:8080/api/media";

    /**
     * Maximum number of media files per post
     */
    @Positive
    private int maxFilesPerPost = 10;
}