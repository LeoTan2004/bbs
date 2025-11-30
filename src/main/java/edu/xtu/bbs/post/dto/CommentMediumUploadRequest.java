package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for comment medium upload request
 */
public record CommentMediumUploadRequest(
        @NotNull String type,
        @NotNull @Min(1) Long size
) implements Serializable {
}