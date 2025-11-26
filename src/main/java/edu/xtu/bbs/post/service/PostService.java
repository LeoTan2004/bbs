package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.DraftContentEditor;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Post Service Interface
 * <p>
 * Handles operations related to posts, including draft management,
 * publishing, and retrieval of published posts.
 * </p>
 * <p>
 * If you want to manage post media, please refer to {@link PostMediumService}
 */
public interface PostService {

    // region Draft Post Processing

    /**
     * Get draft post for a user
     * <p>
     * if exists， return the latest only one,
     * if not exists, create a new draft post and return it
     * </p>
     *
     * @param userId The user ID
     * @return The draft post
     */
    Post getDraft(Integer userId);

    /**
     * edit draft post content
     * <p>
     * only title, content and category can be edited,
     * media should be managed by {@link PostMediumService}
     * </p>
     *
     * @param userId             the user id
     * @param draftId            the draft post id
     * @param draftContentEditor the draft content editor dto
     * @return The edited draft post
     * @throws PostNotFoundException         when the draft post is not found
     * @throws ModifyNotPermittedException   when the user doesn't have permission to modify the draft
     * @throws PostStatusNotAllowedException when the post is not in draft status
     * @throws SensitiveContentException     when the content contains sensitive material
     */
    Post editDraft(Integer userId, Integer draftId, DraftContentEditor draftContentEditor)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException;

    /**
     * publish draft post
     *
     * @param userId  the user id
     * @param draftId the draft post id
     * @return The published post
     * @throws PostNotFoundException         when the draft post is not found
     * @throws ModifyNotPermittedException   when the user doesn't have permission to publish the draft
     * @throws PostStatusNotAllowedException when the post is not in draft status
     * @throws SensitiveContentException     when the content contains sensitive material
     */
    Post publishDraft(Integer userId, Integer draftId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException;

    // endregion

    // region Published Post Reading

    /**
     * Get user's all published posts
     *
     * @param userId   The user ID
     * @param pageable Pagination information
     * @return Paginated list of published posts by the user
     */
    Page<Post> getPublishedPostsByUser(Integer userId, Pageable pageable);

    /**
     * Get all published posts by category
     *
     * @param category the category
     * @param pageable Pagination information
     * @return Paginated list of published posts by category
     */
    Page<Post> getAllPublishedPostsByCategory(String category, Pageable pageable);

    /**
     * Get published post by id
     *
     * @param postId the post id
     * @return the published post， null if not found or not published
     */
    Post getPublishedPostById(Integer postId);

    /**
     * Delete published post by id
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if deleted successfully, false otherwise
     * @throws PostNotFoundException       when the post is not found
     * @throws ModifyNotPermittedException when the user doesn't have permission to delete the post
     * @throws DeletedFailedException      when the deletion operation fails
     */
    Boolean deletePost(Integer userId, Integer postId)
            throws PostNotFoundException, ModifyNotPermittedException, DeletedFailedException;

    // endregion
}
