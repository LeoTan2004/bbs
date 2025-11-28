package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;

import java.util.List;
import java.util.Map;

/**
 * Post Batch Operation Service Interface
 * <p>
 * Handles batch operations for better performance when dealing with multiple posts.
 * </p>
 */
public interface PostBatchService {

    /**
     * Batch like posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to like
     * @return map of postId to operation result (true if now liked, false if already liked)
     * @throws PostNotFoundException         when any post is not found
     * @throws PostStatusNotAllowedException when any post is not in published status
     */
    Map<Integer, Boolean> batchLikePosts(Integer userId, List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Batch unlike posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to unlike
     * @return map of postId to operation result (true if was unliked, false if already not liked)
     * @throws PostNotFoundException         when any post is not found
     * @throws PostStatusNotAllowedException when any post is not in published status
     */
    Map<Integer, Boolean> batchUnlikePosts(Integer userId, List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Batch favorite posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to favorite
     * @return map of postId to operation result (true if now favorited, false if already favorited)
     * @throws PostNotFoundException         when any post is not found
     * @throws PostStatusNotAllowedException when any post is not in published status
     */
    Map<Integer, Boolean> batchFavorPosts(Integer userId, List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Batch unfavorite posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to unfavorite
     * @return map of postId to operation result (true if was unfavorited, false if already not favorited)
     * @throws PostNotFoundException         when any post is not found
     * @throws PostStatusNotAllowedException when any post is not in published status
     */
    Map<Integer, Boolean> batchUnfavorPosts(Integer userId, List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException;

    /**
     * Check like status for multiple posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to check
     * @return map of postId to like status
     */
    Map<Integer, Boolean> batchCheckLikeStatus(Integer userId, List<Integer> postIds);

    /**
     * Check favor status for multiple posts
     *
     * @param userId  the user id
     * @param postIds the list of post ids to check
     * @return map of postId to favor status
     */
    Map<Integer, Boolean> batchCheckFavorStatus(Integer userId, List<Integer> postIds);
}