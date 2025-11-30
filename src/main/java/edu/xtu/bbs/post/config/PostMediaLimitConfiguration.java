package edu.xtu.bbs.post.config;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Global media limitation configuration for posts and comments.
 * Allows tuning the maximum number of media attachments without touching code.
 */
@Component
@ConfigurationProperties(prefix = "bbs.post.media-limits")
@Data
public class PostMediaLimitConfiguration {

    /**
     * Maximum number of media attachments allowed per post.
     */
    @Positive
    private int maxPostMedia = 10;

    /**
     * Maximum number of media attachments allowed per comment.
     */
    @Positive
    private int maxCommentMedia = 5;
}
