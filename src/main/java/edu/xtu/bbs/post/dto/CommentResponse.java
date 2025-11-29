package edu.xtu.bbs.post.dto;

import edu.xtu.bbs.post.model.Medium;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for comment response
 */
public record CommentResponse(
        Integer id,
        Integer postId,
        Integer userId,
        String username,
        String userAvatar,
        Integer parentCommentId,
        String content,
        List<Medium> media,
        Integer comments,
        Integer likes,
        Instant createdAt,
        Instant updatedAt,
        Boolean isLiked
) implements Serializable {
}