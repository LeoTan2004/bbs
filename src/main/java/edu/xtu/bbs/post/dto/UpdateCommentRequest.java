package edu.xtu.bbs.post.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for updating a comment
 */
public record UpdateCommentRequest(
        @ContentAudit @NotBlank @Size(max = 2000) String content
) implements Serializable {
}