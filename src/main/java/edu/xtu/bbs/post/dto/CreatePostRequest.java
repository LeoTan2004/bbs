package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for creating a published post without draft workflow
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @Size(max = 100) String category
) implements Serializable {
}
