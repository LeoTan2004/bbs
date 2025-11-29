package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.user.exception.UserNotFoundException;

/**
 * 评论点赞服务接口
 * 
 * @author BBS Team
 */
public interface CommentLikeService {

    /**
     * Check if a user has already liked a specific comment
     *
     * @param userId user ID
     * @param commentId comment ID
     * @return true if already liked, false otherwise
     */
    boolean hasLikedComment(Integer userId, Integer commentId);

    /**
     * User likes a comment
     *
     * @param userId user ID
     * @param commentId comment ID
     * @throws UserNotFoundException user does not exist
     * @throws CommentNotFoundException comment does not exist
     * @throws IllegalStateException duplicate like
     */
    void likeComment(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException, IllegalStateException;

    /**
     * User unlikes a comment
     *
     * @param userId user ID
     * @param commentId comment ID
     * @throws UserNotFoundException user does not exist
     * @throws CommentNotFoundException comment does not exist
     * @throws IllegalStateException cannot unlike when not liked
     */
    void unlikeComment(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException, IllegalStateException;

    /**
     * Get the number of likes for a comment
     *
     * @param commentId comment ID
     * @return number of likes
     */
    long getCommentLikeCount(Integer commentId);

    /**
     * Toggle comment like status (unlike if liked, like if not liked)
     *
     * @param userId user ID
     * @param commentId comment ID
     * @return true if liked after operation, false if not liked
     * @throws UserNotFoundException user does not exist
     * @throws CommentNotFoundException comment does not exist
     */
    boolean toggleCommentLike(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException;
}