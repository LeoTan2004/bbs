package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PostFavorite;
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
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Integer> {

    // Basic query methods using Spring Data JPA naming convention

    /**
     * Find a favorite record by post and user
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return Optional PostFavorite if exists
     */
    Optional<PostFavorite> findByPostIdAndUserId(Integer postId, Integer userId);

    /**
     * Check if a user has favorite a specific post
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return True if the user has favorite the post
     */
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    /**
     * Find all favorites by a specific user with pagination
     *
     * @param userId   The user ID
     * @param pageable Pagination information
     * @return Paginated list of favorites by the user
     */
    Page<PostFavorite> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * Find all favorites for a specific post with pagination
     *
     * @param postId   The post ID
     * @param pageable Pagination information
     * @return Paginated list of favorites for the post
     */
    Page<PostFavorite> findByPostIdOrderByCreatedAtDesc(Integer postId, Pageable pageable);

    /**
     * Count total favorites for a specific post
     *
     * @param postId The post ID
     * @return Number of favorites for the post
     */
    Long countByPostId(Integer postId);

    /**
     * Count total favorites by a specific user
     *
     * @param userId The user ID
     * @return Number of favorites made by the user
     */
    Long countByUserId(Integer userId);

    /**
     * Find favorites created after a specific time
     *
     * @param createdAt The timestamp threshold
     * @param pageable  Pagination information
     * @return Paginated list of recent favorites
     */
    Page<PostFavorite> findByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt, Pageable pageable);

    // Custom JPQL queries for more complex operations

    /**
     * Find users who are a favorite a specific post
     *
     * @param postId The post ID
     * @return List of user IDs who is a favorite the post
     */
    @Query("SELECT pf.user.id FROM PostFavorite pf WHERE pf.post.id = :postId ORDER BY pf.createdAt DESC")
    List<Integer> findUserIdsByPostId(@Param("postId") Integer postId);

    /**
     * Find posts favorite by a user in a specific time range
     *
     * @param userId    The user ID
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @param pageable  Pagination information
     * @return Paginated list of posts favorite by the user in the time range
     */
    @Query("SELECT pf FROM PostFavorite pf " +
            "WHERE pf.user.id = :userId " +
            "AND pf.createdAt BETWEEN :startTime AND :endTime " +
            "ORDER BY pf.createdAt DESC")
    Page<PostFavorite> findUserFavoritesInTimeRange(@Param("userId") Integer userId,
                                                    @Param("startTime") Instant startTime,
                                                    @Param("endTime") Instant endTime,
                                                    Pageable pageable);

    /**
     * Find most favorite posts in a time range
     *
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @param pageable  Pagination information
     * @return List of post IDs sorted by favorite count
     */
    @Query("SELECT pf.post.id, COUNT(pf) as favoriteCount FROM PostFavorite pf " +
            "WHERE pf.createdAt BETWEEN :startTime AND :endTime " +
            "GROUP BY pf.post.id " +
            "ORDER BY favoriteCount DESC")
    Page<Object[]> findMostFavoritePostsInTimeRange(@Param("startTime") Instant startTime,
                                                     @Param("endTime") Instant endTime,
                                                     Pageable pageable);

    /**
     * Get favorite statistics for multiple posts
     *
     * @param postIds List of post IDs
     * @return List of arrays containing [postId, favoriteCount]
     */
    @Query("SELECT pf.post.id, COUNT(pf) FROM PostFavorite pf " +
            "WHERE pf.post.id IN :postIds " +
            "GROUP BY pf.post.id")
    List<Object[]> getFavoriteCountsForPosts(@Param("postIds") List<Integer> postIds);

    /**
     * Find users who are a favorite posts by a specific author
     *
     * @param authorId The author's ID
     * @param pageable Pagination information
     * @return Paginated list of user IDs who favorited the author's posts
     */
    @Query("SELECT DISTINCT pf.user.id FROM PostFavorite pf " +
            "WHERE pf.post.author.id = :authorId " +
            "ORDER BY pf.createdAt DESC")
    Page<Integer> findUsersWhoFavoriteAuthorPosts(@Param("authorId") Integer authorId, Pageable pageable);

    /**
     * Count favorites received by an author in a time range
     *
     * @param authorId  The author's ID
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @return Number of favorites received by the author's posts
     */
    @Query("SELECT COUNT(pf) FROM PostFavorite pf " +
            "WHERE pf.post.author.id = :authorId " +
            "AND pf.createdAt BETWEEN :startTime AND :endTime")
    Long countFavoritesReceivedByAuthorInTimeRange(@Param("authorId") Integer authorId,
                                                   @Param("startTime") Instant startTime,
                                                   @Param("endTime") Instant endTime);

    /**
     * Find posts that are both liked and favorite by a user
     *
     * @param userId The user ID
     * @return List of post IDs that are both liked and favorited by the user
     */
    @Query("SELECT pf.post.id FROM PostFavorite pf " +
            "WHERE pf.user.id = :userId " +
            "AND pf.post.id IN (SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId)")
    List<Integer> findPostsBothLikedAndFavorite(@Param("userId") Integer userId);

    /**
     * Find favorited posts for a user within a candidate id set
     */
    @Query("SELECT pf.post.id FROM PostFavorite pf WHERE pf.user.id = :userId AND pf.post.id IN :postIds")
    List<Integer> findPostIdsByUserIdAndPostIdIn(@Param("userId") Integer userId,
                                                 @Param("postIds") List<Integer> postIds);

    /**
     * Find user's favorite posts by category
     *
     * @param userId   The user ID
     * @param category The category name
     * @param pageable Pagination information
     * @return Paginated list of user's favorites in the specified category
     */
    @Query("SELECT pf FROM PostFavorite pf " +
            "WHERE pf.user.id = :userId " +
            "AND pf.post.category = :category " +
            "ORDER BY pf.createdAt DESC")
    Page<PostFavorite> findUserFavoritesByCategory(@Param("userId") Integer userId,
                                                   @Param("category") String category,
                                                   Pageable pageable);

    /**
     * Find trending favorites (recent favorites on recently created posts)
     *
     * @param recentPostsThreshold     Threshold for recent posts
     * @param recentFavoritesThreshold Threshold for recent favorites
     * @param pageable                 Pagination information
     * @return Paginated list of trending favorites
     */
    @Query("SELECT pf FROM PostFavorite pf " +
            "WHERE pf.post.createdAt >= :recentPostsThreshold " +
            "AND pf.createdAt >= :recentFavoritesThreshold " +
            "AND pf.post.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "ORDER BY pf.createdAt DESC")
    Page<PostFavorite> findTrendingFavorites(@Param("recentPostsThreshold") Instant recentPostsThreshold,
                                             @Param("recentFavoritesThreshold") Instant recentFavoritesThreshold,
                                             Pageable pageable);

    /**
     * Delete a specific favorite by post and user
     *
     * @param postId The post ID
     * @param userId The user ID
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM PostFavorite pf WHERE pf.post.id = :postId AND pf.user.id = :userId")
    int deleteByPostIdAndUserId(@Param("postId") Integer postId, @Param("userId") Integer userId);

    /**
     * Delete all favorites for posts older than specified date
     *
     * @param cutoffDate The cutoff date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM PostFavorite pf WHERE pf.post.createdAt < :cutoffDate")
    int deleteOldPostFavorites(@Param("cutoffDate") Instant cutoffDate);
}