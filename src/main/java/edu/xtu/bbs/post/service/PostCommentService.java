package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.CommentResponse;
import edu.xtu.bbs.post.dto.CreateCommentRequest;
import edu.xtu.bbs.post.dto.DraftCommentEditor;
import edu.xtu.bbs.post.dto.UpdateCommentRequest;
import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.exception.CommentPermissionDeniedException;
import edu.xtu.bbs.post.exception.CommentStatusNotAllowedException;
import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.post.model.PostComment;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Post Comment Service Interface
 * <p>
 * Handles operations related to post comments, following similar patterns as PostService.
 * Comments can have media attachments managed through PostCommentMediumService.
 * </p>
 */
public interface PostCommentService {

    // region Comment CRUD Operations

    /**
     * Create a new comment on a post
     *
     * @param userId the user id creating the comment
     * @param request the comment creation request
     * @return the created comment
     * @throws PostNotFoundException when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     * @throws CommentNotFoundException when parent comment is not found (if specified)
     */
    PostComment createComment(@NotNull Integer userId, @NotNull CreateCommentRequest request)
            throws PostNotFoundException, PostStatusNotAllowedException, CommentNotFoundException;

    /**
     * Update an existing comment (only content can be updated, media managed separately)
     *
     * @param userId the user id updating the comment
     * @param commentId the comment id to update
     * @param request the comment update request
     * @return the updated comment
     * @throws CommentNotFoundException when the comment is not found
     * @throws CommentPermissionDeniedException when user is not authorized to update the comment
     */
    PostComment updateComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull UpdateCommentRequest request)
            throws CommentNotFoundException, CommentPermissionDeniedException;

    /**
     * Delete a comment
     *
     * @param userId the user id deleting the comment
     * @param commentId the comment id to delete
     * @throws CommentNotFoundException when the comment is not found
     * @throws CommentPermissionDeniedException when user is not authorized to delete the comment
     */
    void deleteComment(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, CommentPermissionDeniedException;

    // endregion

    // region Comment Retrieval

    /**
     * Get a comment by id
     *
     * @param commentId the comment id
     * @return the comment, null if not found
     */
    PostComment getCommentById(@NotNull Integer commentId);

    /**
     * Get a comment response by id (with user interaction info)
     *
     * @param commentId the comment id
     * @param currentUserId the current user id (for checking interactions)
     * @return the comment response
     * @throws CommentNotFoundException when the comment is not found
     */
    CommentResponse getCommentResponseById(@NotNull Integer commentId, Integer currentUserId)
            throws CommentNotFoundException;

    /**
     * Get paginated top-level comments for a post
     *
     * @param postId the post id
     * @param pageable pagination information
     * @return paginated list of top-level comments
     * @throws PostNotFoundException when the post is not found
     */
    Page<PostComment> getCommentsByPost(@NotNull Integer postId, @NotNull Pageable pageable)
            throws PostNotFoundException;

    /**
     * Get paginated comment responses for a post (with user interaction info)
     *
     * @param postId the post id
     * @param currentUserId the current user id (for checking interactions)
     * @param pageable pagination information
     * @return paginated list of comment responses
     * @throws PostNotFoundException when the post is not found
     */
    Page<CommentResponse> getCommentResponsesByPost(@NotNull Integer postId, Integer currentUserId, @NotNull Pageable pageable)
            throws PostNotFoundException;

    /**
     * Get paginated replies for a parent comment
     *
     * @param parentCommentId the parent comment id
     * @param pageable pagination information
     * @return paginated list of reply comments
     * @throws CommentNotFoundException when the parent comment is not found
     */
    Page<PostComment> getRepliesByComment(@NotNull Integer parentCommentId, @NotNull Pageable pageable)
            throws CommentNotFoundException;

    /**
     * Get paginated reply responses for a parent comment (with user interaction info)
     *
     * @param parentCommentId the parent comment id
     * @param currentUserId the current user id (for checking interactions)
     * @param pageable pagination information
     * @return paginated list of reply responses
     * @throws CommentNotFoundException when the parent comment is not found
     */
    Page<CommentResponse> getReplyResponsesByComment(@NotNull Integer parentCommentId, Integer currentUserId, @NotNull Pageable pageable)
            throws CommentNotFoundException;

    /**
     * Get paginated comments created by a user
     *
     * @param userId the user id
     * @param pageable pagination information
     * @return paginated list of comments created by the user
     */
    Page<PostComment> getCommentsByUser(@NotNull Integer userId, @NotNull Pageable pageable);

    /**
     * Get paginated comment responses created by a user (with user interaction info)
     *
     * @param userId the user id
     * @param currentUserId the current user id (for checking interactions)
     * @param pageable pagination information
     * @return paginated list of comment responses created by the user
     */
    Page<CommentResponse> getCommentResponsesByUser(@NotNull Integer userId, Integer currentUserId, @NotNull Pageable pageable);

    // endregion

    // region Statistics

    /**
     * Count total comments for a post (including replies)
     *
     * @param postId the post id
     * @return the number of comments for the post
     */
    Integer countCommentsByPost(@NotNull Integer postId);

    /**
     * Count top-level comments for a post (excluding replies)
     *
     * @param postId the post id
     * @return the number of top-level comments for the post
     */
    Integer countTopLevelCommentsByPost(@NotNull Integer postId);

    /**
     * Count replies for a parent comment
     *
     * @param parentCommentId the parent comment id
     * @return the number of replies to the comment
     */
    Integer countRepliesByComment(@NotNull Integer parentCommentId);

    /**
     * Count comments created by a user
     *
     * @param userId the user id
     * @return the number of comments created by the user
     */
    Integer countCommentsByUser(@NotNull Integer userId);

    // endregion

    // region Draft Comment Operations

    /**
     * Get or create user's comment draft box
     * <p>
     * If user doesn't have a draft, creates a new empty one.
     * If user has a draft but for different post/parent, overwrites it with new target.
     * </p>
     *
     * @param userId the user id
     * @param postId the target post id
     * @param parentCommentId the parent comment id (null for top-level comment)
     * @return the user's comment draft
     * @throws PostNotFoundException when the target post is not found
     * @throws CommentNotFoundException when the parent comment is not found (if specified)
     */
    PostComment getDraftBox(@NotNull Integer userId, @NotNull Integer postId, Integer parentCommentId)
            throws PostNotFoundException, CommentNotFoundException;

    /**
     * Edit user's comment draft content
     *
     * @param userId the user id
     * @param draftEditor the draft content editor
     * @return the updated draft comment
     * @throws PostNotFoundException when the target post is not found
     * @throws CommentNotFoundException when the parent comment is not found (if specified)
     * @throws CommentPermissionDeniedException when user doesn't own the draft
     */
    PostComment editDraft(@NotNull Integer userId, @NotNull DraftCommentEditor draftEditor)
            throws PostNotFoundException, CommentNotFoundException, CommentPermissionDeniedException;

    /**
     * Publish user's comment draft
     *
     * @param userId the user id
     * @return the published comment
     * @throws CommentNotFoundException when user doesn't have a draft
     * @throws CommentPermissionDeniedException when user doesn't own the draft
     * @throws CommentStatusNotAllowedException when the draft is not in valid state for publishing
     * @throws PostStatusNotAllowedException when the target post is not published
     */
    PostComment publishDraft(@NotNull Integer userId)
            throws CommentNotFoundException, CommentPermissionDeniedException, CommentStatusNotAllowedException, PostStatusNotAllowedException;

    /**
     * Clear user's comment draft
     *
     * @param userId the user id
     * @throws CommentNotFoundException when user doesn't have a draft
     * @throws CommentPermissionDeniedException when user doesn't own the draft
     */
    void clearDraft(@NotNull Integer userId)
            throws CommentNotFoundException, CommentPermissionDeniedException;

    /**
     * Check if user has a comment draft
     *
     * @param userId the user id
     * @return true if user has a draft
     */
    Boolean hasDraft(@NotNull Integer userId);

    // endregion

    // region Comment Interaction Operations

    /**
     * Toggle comment like (like if not liked, unlike if liked)
     *
     * @param userId the user id
     * @param commentId the comment id
     * @return true if comment is now liked, false if now unliked
     * @throws CommentNotFoundException when the comment is not found
     */
    Boolean toggleCommentLike(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException;

    /**
     * Like a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @throws CommentNotFoundException when the comment is not found
     * @throws IllegalStateException when user has already liked the comment
     */
    void likeComment(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException, IllegalStateException;

    /**
     * Unlike a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @throws CommentNotFoundException when the comment is not found
     * @throws IllegalStateException when user has not liked the comment
     */
    void unlikeComment(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException, IllegalStateException;

    /**
     * Check if user has liked a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @return true if user has liked the comment
     */
    Boolean hasLikedComment(@NotNull Integer userId, @NotNull Integer commentId);

    /**
     * Get comment like count
     *
     * @param commentId the comment id
     * @return the number of likes for the comment
     */
    Long getCommentLikeCount(@NotNull Integer commentId);

    // endregion

    // region Permission Check

    /**
     * Check if user has permission to modify a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @return true if user has modify permission
     */
    Boolean hasModifyPermission(@NotNull Integer userId, @NotNull Integer commentId);

    // endregion

}
