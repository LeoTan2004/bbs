package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
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
     * Like a post
     *
     * @param userId the user id
     * @param postId the post id
     * @return is liked now
     * @throws PostNotFoundException         when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     */
    Boolean likePost(Integer userId, Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Unlike a post
     *
     * @param userId the user id
     * @param postId the post id
     * @return is unliked now
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
     * Get paginated list of post IDs liked by a user
     *
     * @param userId   the user id
     * @param pageable pagination information
     * @return paginated list of liked post IDs
     */
    Page<Integer> getLikedPostsByUser(Integer userId, Pageable pageable);

    Integer countUserLikedPosts(Integer userId);

}
