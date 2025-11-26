package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Post Favor Service Interface
 * <p>
 * Handles operations related to post favouring.
 * </p>
 */
public interface PostFavorService {

    /**
     * Favorite a post
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if the post is favorite now
     * @throws PostNotFoundException         when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     */
    Boolean favorPost(Integer userId, Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Unfavorite a post
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if the post is unfavorited now
     * @throws PostNotFoundException         when the post is not found
     * @throws PostStatusNotAllowedException when the post is not in published status
     */
    Boolean unfavoredPost(Integer userId, Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Check if a post is favored by a user
     *
     * @param userId the user id
     * @param postId the post id
     * @return true if the post is favorite by the user
     */
    Boolean isPostFavored(Integer userId, Integer postId);

    /**
     * Get paginated list of user's favorite posts
     *
     * @param userId   the user id
     * @param pageable pagination information
     * @return paginated list of favorite posts
     */
    Page<Post> getUserFavoredPosts(Integer userId, Pageable pageable);

    Integer countUserFavoredPosts(Integer userId);

    Integer countPostFavoredByUsers(Integer postId);

}
