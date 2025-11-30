package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.CommentStatus;
import edu.xtu.bbs.post.model.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, Integer> {
    
    /**
     * Find top-level comments (no parent) for a post
     *
     * @param postId the post id
     * @param pageable pagination information
     * @return paginated comments
     */
    Page<PostComment> findByPostIdAndParentPostIdIsNull(Integer postId, Pageable pageable);
    
    /**
     * Find replies for a parent comment
     *
     * @param parentPostId the parent comment id
     * @param pageable pagination information
     * @return paginated reply comments
     */
    Page<PostComment> findByParentPostId(Integer parentPostId, Pageable pageable);
    
    /**
     * Find comments by user
     *
     * @param userId the user id
     * @param pageable pagination information
     * @return paginated comments
     */
    Page<PostComment> findByUserId(Integer userId, Pageable pageable);
    
    /**
     * Count all comments for a post (including replies)
     *
     * @param postId the post id
     * @return comment count
     */
    Long countByPostId(Integer postId);
    
    /**
     * Count top-level comments for a post (excluding replies)
     *
     * @param postId the post id
     * @return top-level comment count
     */
    Long countByPostIdAndParentPostIdIsNull(Integer postId);
    
    /**
     * Count comments by user
     *
     * @param userId the user id
     * @return comment count
     */
    Long countByUserId(Integer userId);
    
    /**
     * Count replies for a parent comment
     *
     * @param parentPostId the parent comment id
     * @return reply count
     */
    Long countByParentPostId(Integer parentPostId);
    
    /**
     * Check if a comment exists
     *
     * @param id the comment id
     * @return true if exists, false otherwise
     */
    boolean existsById(Integer id);
    
    // region Draft-related methods
    
    /**
     * Find user's comment draft
     *
     * @param userId the user id
     * @return the draft comment if exists
     */
    Optional<PostComment> findByUserIdAndStatus(Integer userId, CommentStatus status);
    
    /**
     * Find published comments for a post
     *
     * @param postId the post id
     * @param status the comment status
     * @param pageable pagination information
     * @return paginated published comments
     */
    @EntityGraph(attributePaths = {"user", "post"})
    Page<PostComment> findByPostIdAndParentPostIdIsNullAndStatus(Integer postId, CommentStatus status, Pageable pageable);
    
    /**
     * Find published replies for a parent comment
     *
     * @param parentPostId the parent comment id
     * @param status the comment status
     * @param pageable pagination information
     * @return paginated published reply comments
     */
    @EntityGraph(attributePaths = {"user", "post"})
    Page<PostComment> findByParentPostIdAndStatus(Integer parentPostId, CommentStatus status, Pageable pageable);
    
    /**
     * Find published comments by user
     *
     * @param userId the user id
     * @param status the comment status
     * @param pageable pagination information
     * @return paginated published comments
     */
    @EntityGraph(attributePaths = {"user", "post"})
    Page<PostComment> findByUserIdAndStatus(Integer userId, CommentStatus status, Pageable pageable);
    
    /**
     * Count published comments for a post (including replies)
     *
     * @param postId the post id
     * @param status the comment status
     * @return published comment count
     */
    Long countByPostIdAndStatus(Integer postId, CommentStatus status);
    
    /**
     * Count published top-level comments for a post (excluding replies)
     *
     * @param postId the post id
     * @param status the comment status
     * @return published top-level comment count
     */
    Long countByPostIdAndParentPostIdIsNullAndStatus(Integer postId, CommentStatus status);
    
    /**
     * Count published comments by user
     *
     * @param userId the user id
     * @param status the comment status
     * @return published comment count
     */
    Long countByUserIdAndStatus(Integer userId, CommentStatus status);
    
    /**
     * Count published replies for a parent comment
     *
     * @param parentPostId the parent comment id
     * @param status the comment status
     * @return published reply count
     */
    Long countByParentPostIdAndStatus(Integer parentPostId, CommentStatus status);
    
    // endregion
}