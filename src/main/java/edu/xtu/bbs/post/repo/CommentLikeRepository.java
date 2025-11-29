package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 评论点赞Repository
 * 
 * @author BBS Team
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Integer> {

    /**
     * Check if a user has already liked a specific comment
     *
     * @param commentId comment ID
     * @param userId user ID
     * @return true if already liked, false otherwise
     */
    boolean existsByCommentIdAndUserId(Integer commentId, Integer userId);

    /**
     * Get the number of likes for a specific comment
     *
     * @param commentId comment ID
     * @return number of likes
     */
    long countByCommentId(Integer commentId);

    /**
     * Find a user's like record for a specific comment
     *
     * @param commentId comment ID
     * @param userId user ID
     * @return like record (if exists)
     */
    Optional<CommentLike> findByCommentIdAndUserId(Integer commentId, Integer userId);

    /**
     * Delete a user's like record for a specific comment
     *
     * @param commentId comment ID
     * @param userId user ID
     */
    void deleteByCommentIdAndUserId(Integer commentId, Integer userId);
}