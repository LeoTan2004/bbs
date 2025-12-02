package edu.xtu.bbs.post.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for creating a comment
 */
public record CreateCommentRequest(
        @NotNull Integer postId,
        @ContentAudit @NotBlank @Size(max = 2000) String content,
        Integer parentCommentId
) implements Serializable {
}