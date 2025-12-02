package edu.xtu.bbs.post.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for {@link edu.xtu.bbs.post.model.Post}
 */
public record DraftContentEditor(@ContentAudit @NotNull @Size(max = 255) String title,
                                 @ContentAudit String content,
                                 @Size(max = 100) String category) implements Serializable {
}