package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

    // Basic query methods using Spring Data JPA naming convention

    /**
     * Find a like record by post and user
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return Optional PostLike if exists
     */
    Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);

    /**
     * Check if a user has liked a specific post
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return True if the user has liked the post
     */
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    /**
     * Find all likes by a specific user with pagination
     *
     * @param userId   The user ID
     * @param pageable Pagination information
     * @return Paginated list of likes by the user
     */
    Page<PostLike> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * Find all likes for a specific post with pagination
     *
     * @param postId   The post ID
     * @param pageable Pagination information
     * @return Paginated list of likes for the post
     */
    Page<PostLike> findByPostIdOrderByCreatedAtDesc(Integer postId, Pageable pageable);

    /**
     * Count total likes for a specific post
     *
     * @param postId The post ID
     * @return Number of likes for the post
     */
    Long countByPostId(Integer postId);

    /**
     * Count total likes by a specific user
     *
     * @param userId The user ID
     * @return Number of likes made by the user
     */
    Long countByUserId(Integer userId);

    /**
     * Find likes created after a specific time
     *
     * @param createdAt The timestamp threshold
     * @param pageable  Pagination information
     * @return Paginated list of recent likes
     */
    Page<PostLike> findByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt, Pageable pageable);

    // Custom JPQL queries for more complex operations

    /**
     * Find users who liked a specific post
     *
     * @param postId The post ID
     * @return List of user IDs who liked the post
     */
    @Query("SELECT pl.user.id FROM PostLike pl WHERE pl.post.id = :postId ORDER BY pl.createdAt DESC")
    List<Integer> findUserIdsByPostId(@Param("postId") Integer postId);

    /**
     * Find posts liked by a user in a specific time range
     *
     * @param userId    The user ID
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @param pageable  Pagination information
     * @return Paginated list of posts liked by the user in the time range
     */
    @Query("SELECT pl FROM PostLike pl " +
            "WHERE pl.user.id = :userId " +
            "AND pl.createdAt BETWEEN :startTime AND :endTime " +
            "ORDER BY pl.createdAt DESC")
    Page<PostLike> findUserLikesInTimeRange(@Param("userId") Integer userId,
                                            @Param("startTime") Instant startTime,
                                            @Param("endTime") Instant endTime,
                                            Pageable pageable);

    /**
     * Find most liked posts in a time range
     *
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @param pageable  Pagination information
     * @return List of post IDs sorted by like count
     */
    @Query("SELECT pl.post.id, COUNT(pl) as likeCount FROM PostLike pl " +
            "WHERE pl.createdAt BETWEEN :startTime AND :endTime " +
            "GROUP BY pl.post.id " +
            "ORDER BY likeCount DESC")
    Page<Object[]> findMostLikedPostsInTimeRange(@Param("startTime") Instant startTime,
                                                 @Param("endTime") Instant endTime,
                                                 Pageable pageable);

    /**
     * Get like statistics for multiple posts
     *
     * @param postIds List of post IDs
     * @return List of arrays containing [postId, likeCount]
     */
    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl " +
            "WHERE pl.post.id IN :postIds " +
            "GROUP BY pl.post.id")
    List<Object[]> getLikeCountsForPosts(@Param("postIds") List<Integer> postIds);

    /**
     * Find users who liked posts by a specific author
     *
     * @param authorId The author's ID
     * @param pageable Pagination information
     * @return Paginated list of user IDs who liked the author's posts
     */
    @Query("SELECT DISTINCT pl.user.id FROM PostLike pl " +
            "WHERE pl.post.author.id = :authorId " +
            "ORDER BY pl.createdAt DESC")
    Page<Integer> findUsersWhoLikedAuthorPosts(@Param("authorId") Integer authorId, Pageable pageable);

    /**
     * Count likes received by an author in a time range
     *
     * @param authorId  The author's ID
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @return Number of likes received by the author's posts
     */
    @Query("SELECT COUNT(pl) FROM PostLike pl " +
            "WHERE pl.post.author.id = :authorId " +
            "AND pl.createdAt BETWEEN :startTime AND :endTime")
    Long countLikesReceivedByAuthorInTimeRange(@Param("authorId") Integer authorId,
                                               @Param("startTime") Instant startTime,
                                               @Param("endTime") Instant endTime);

    /**
     * Find mutual likes between two users (posts they both liked)
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of post IDs both users liked
     */
    @Query("SELECT pl1.post.id FROM PostLike pl1 " +
            "JOIN PostLike pl2 ON pl1.post.id = pl2.post.id " +
            "WHERE pl1.user.id = :userId1 AND pl2.user.id = :userId2")
    List<Integer> findMutualLikes(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    /**
     * Find liked posts for a user within a candidate id set
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    List<Integer> findPostIdsByUserIdAndPostIdIn(@Param("userId") Integer userId,
                                                 @Param("postIds") List<Integer> postIds);

    /**
     * Delete a specific like by post and user
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId AND pl.user.id = :userId")
    int deleteByPostIdAndUserId(@Param("postId") Integer postId, @Param("userId") Integer userId);

    /**
     * Delete all likes for posts older than specified date
     *
     * @param cutoffDate The cutoff date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.createdAt < :cutoffDate")
    int deleteOldPostLikes(@Param("cutoffDate") Instant cutoffDate);
}