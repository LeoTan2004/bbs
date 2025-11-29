package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for comment medium upload request
 */
public record CommentMediumUploadRequest(
        @NotNull String type,
        @NotNull @Size(min = 1) Long size
) implements Serializable {
}