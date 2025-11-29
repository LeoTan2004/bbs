package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Draft Comment Content Editor DTO
 * <p>
 * Used for editing the user's comment draft box.
 * When postId or parentCommentId changes, the previous draft content will be overwritten.
 * </p>
 */
public record DraftCommentEditor(
    @NotNull Integer postId,
    Integer parentCommentId,  // null for top-level comment, non-null for reply
    String content
) {
    
    /**
     * Check if this draft is for a top-level comment
     * @return true if this is a top-level comment draft
     */
    public boolean isTopLevelComment() {
        return parentCommentId == null;
    }
    
    /**
     * Check if this draft has the same target as another draft
     * @param other the other draft to compare
     * @return true if they target the same post and parent comment
     */
    public boolean hasSameTarget(DraftCommentEditor other) {
        if (other == null) return false;
        return postId.equals(other.postId) && 
               ((parentCommentId == null && other.parentCommentId == null) ||
                (parentCommentId != null && parentCommentId.equals(other.parentCommentId)));
    }
}