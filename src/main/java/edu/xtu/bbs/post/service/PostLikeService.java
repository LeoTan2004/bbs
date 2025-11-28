package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Post Like Service Interface
 * <p>
 * Handles operations related to liking posts.
 * </p>
 */
public interface PostLikeService {

    /**
     * Like a post (idempotent operation)
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if post is now liked, false if was already liked
     * @throws PostNotFoundException         when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     */
    Boolean likePost(Integer userId, Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Unlike a post (idempotent operation)
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if post was unliked, false if was already not liked
     * @throws PostNotFoundException         when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     */
    Boolean unlikePost(Integer userId, Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Check if a user has liked a post
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if liked, false otherwise
     */
    Boolean hasLikedPost(Integer userId, Integer postId);

    /**
     * Get paginated list of posts liked by a user
     *
     * @param userId   the user id
     * @param pageable pagination information
     * @return paginated list of liked posts
     */
    Page<Post> getLikedPostsByUser(Integer userId, Pageable pageable);

    Integer countUserLikedPosts(Integer userId);

    Integer countPostLikedByUsers(Integer postId);

}
