package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for updating a comment
 */
public record UpdateCommentRequest(
        @NotBlank @Size(max = 2000) String content
) implements Serializable {
}