package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PublicMatric;
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
public interface PublicMatricRepository extends JpaRepository<PublicMatric, Integer> {

    // Basic query methods using Spring Data JPA naming convention

    /**
     * Find public metrics by post ID
     *
     * @param postId The post ID
     * @return Optional PublicMatric if exists
     */
    Optional<PublicMatric> findByPostId(Integer postId);

    /**
     * Find metrics for posts with views greater than threshold
     *
     * @param minViews Minimum number of views
     * @param pageable Pagination information
     * @return Paginated list of metrics for popular posts
     */
    Page<PublicMatric> findByViewsGreaterThanOrderByViewsDesc(Integer minViews, Pageable pageable);

    /**
     * Find metrics for posts with likes greater than threshold
     *
     * @param minLikes Minimum number of likes
     * @param pageable Pagination information
     * @return Paginated list of metrics for well-liked posts
     */
    Page<PublicMatric> findByLikesGreaterThanOrderByLikesDesc(Integer minLikes, Pageable pageable);

    /**
     * Find metrics for posts with favorites greater than threshold
     *
     * @param minFavorites Minimum number of favorites
     * @param pageable     Pagination information
     * @return Paginated list of metrics for favorited posts
     */
    Page<PublicMatric> findByFavoritesGreaterThanOrderByFavoritesDesc(Integer minFavorites, Pageable pageable);

    // Custom JPQL queries for analytics and statistics

    /**
     * Get total engagement metrics (views + likes + favorites) for a post
     *
     * @param postId The post ID
     * @return Total engagement score
     */
    @Query("SELECT (pm.views + pm.likes + pm.favorites) FROM PublicMatric pm WHERE pm.post.id = :postId")
    Optional<Long> getTotalEngagementByPostId(@Param("postId") Integer postId);

    /**
     * Find posts with highest engagement scores
     *
     * @param pageable Pagination information
     * @return Paginated list of posts sorted by total engagement
     */
    @Query("SELECT pm FROM PublicMatric pm " +
            "ORDER BY (pm.views + pm.likes + pm.favorites) DESC")
    Page<PublicMatric> findPostsByHighestEngagement(Pageable pageable);

    /**
     * Get metrics summary for posts by a specific author
     *
     * @param authorId The author's ID
     * @return Array containing [totalViews, totalLikes, totalFavorites]
     */
    @Query("SELECT SUM(pm.views), SUM(pm.likes), SUM(pm.favorites) FROM PublicMatric pm " +
            "WHERE pm.post.author.id = :authorId")
    Object[] getAuthorTotalMetrics(@Param("authorId") Integer authorId);

    /**
     * Find posts with best engagement ratio (likes+favorites per view)
     *
     * @param minViews Minimum views to avoid division by zero
     * @param pageable Pagination information
     * @return Paginated list sorted by engagement ratio
     */
    @Query("SELECT pm FROM PublicMatric pm " +
            "WHERE pm.views >= :minViews " +
            "ORDER BY (CAST(pm.likes + pm.favorites AS float) / pm.views) DESC")
    Page<PublicMatric> findPostsByEngagementRatio(@Param("minViews") Integer minViews, Pageable pageable);

    /**
     * Get metrics for posts in a specific category
     *
     * @param category The category name
     * @return Array containing [totalViews, totalLikes, totalFavorites, avgViews, avgLikes, avgFavorites]
     */
    @Query("SELECT SUM(pm.views), SUM(pm.likes), SUM(pm.favorites), " +
            "AVG(pm.views), AVG(pm.likes), AVG(pm.favorites) " +
            "FROM PublicMatric pm " +
            "WHERE pm.post.category = :category")
    Object[] getCategoryMetricsSummary(@Param("category") String category);

    /**
     * Find posts with trending metrics (high recent engagement)
     *
     * @param since    Time threshold for recent activity
     * @param pageable Pagination information
     * @return Paginated list of posts with trending metrics
     */
    @Query("SELECT pm FROM PublicMatric pm " +
            "WHERE pm.updatedAt >= :since " +
            "ORDER BY (pm.likes + pm.favorites) DESC, pm.views DESC")
    Page<PublicMatric> findTrendingMetrics(@Param("since") Instant since, Pageable pageable);

    /**
     * Get metrics for multiple posts
     *
     * @param postIds List of post IDs
     * @return List of PublicMatric objects for the specified posts
     */
    @Query("SELECT pm FROM PublicMatric pm WHERE pm.post.id IN :postIds")
    List<PublicMatric> findMetricsByPostIds(@Param("postIds") List<Integer> postIds);

    /**
     * Find posts with views in a specific range
     *
     * @param minViews Minimum views
     * @param maxViews Maximum views
     * @param pageable Pagination information
     * @return Paginated list of posts with views in the specified range
     */
    @Query("SELECT pm FROM PublicMatric pm " +
            "WHERE pm.views BETWEEN :minViews AND :maxViews " +
            "ORDER BY pm.views DESC")
    Page<PublicMatric> findPostsByViewsRange(@Param("minViews") Integer minViews,
                                             @Param("maxViews") Integer maxViews,
                                             Pageable pageable);

    /**
     * Get daily metrics summary for a time range
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of arrays containing [date, totalViews, totalLikes, totalFavorites]
     */
    @Query("SELECT DATE(pm.updatedAt), SUM(pm.views), SUM(pm.likes), SUM(pm.favorites) " +
            "FROM PublicMatric pm " +
            "WHERE pm.updatedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(pm.updatedAt) " +
            "ORDER BY DATE(pm.updatedAt)")
    List<Object[]> getDailyMetricsSummary(@Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    /**
     * Find posts with zero engagement (no likes, favorites, or views)
     *
     * @param pageable Pagination information
     * @return Paginated list of posts with no engagement
     */
    @Query("SELECT pm FROM PublicMatric pm " +
            "WHERE pm.views = 0 AND pm.likes = 0 AND pm.favorites = 0 " +
            "ORDER BY pm.createdAt ASC")
    Page<PublicMatric> findPostsWithZeroEngagement(Pageable pageable);

    /**
     * Calculate engagement rate for posts by an author
     *
     * @param authorId The author's ID
     * @return Average engagement rate (likes + favorites per view)
     */
    @Query("SELECT AVG(CASE WHEN pm.views > 0 THEN " +
            "CAST(pm.likes + pm.favorites AS float) / pm.views ELSE 0 END) " +
            "FROM PublicMatric pm " +
            "WHERE pm.post.author.id = :authorId")
    Optional<Double> getAuthorAverageEngagementRate(@Param("authorId") Integer authorId);

    // Update operations for metrics management

    /**
     * Increment view count for a specific post
     *
     * @param postId      The post ID
     * @param incrementBy Number to increment by (default 1)
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.views = pm.views + :incrementBy " +
            "WHERE pm.post.id = :postId")
    int incrementViewCount(@Param("postId") Integer postId, @Param("incrementBy") Integer incrementBy);

    /**
     * Increment like count for a specific post
     *
     * @param postId The post ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.likes = pm.likes + 1 WHERE pm.post.id = :postId")
    int incrementLikeCount(@Param("postId") Integer postId);

    /**
     * Decrement like count for a specific post
     *
     * @param postId The post ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.likes = CASE WHEN pm.likes > 0 THEN pm.likes - 1 ELSE 0 END " +
            "WHERE pm.post.id = :postId")
    int decrementLikeCount(@Param("postId") Integer postId);

    /**
     * Increment favorite count for a specific post
     *
     * @param postId The post ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.favorites = pm.favorites + 1 WHERE pm.post.id = :postId")
    int incrementFavoriteCount(@Param("postId") Integer postId);

    /**
     * Decrement favorite count for a specific post
     *
     * @param postId The post ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.favorites = CASE WHEN pm.favorites > 0 THEN pm.favorites - 1 ELSE 0 END " +
            "WHERE pm.post.id = :postId")
    int decrementFavoriteCount(@Param("postId") Integer postId);

    /**
     * Reset metrics for posts older than specified date
     *
     * @param cutoffDate The cutoff date
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE PublicMatric pm SET pm.views = 0, pm.likes = 0, pm.favorites = 0 " +
            "WHERE pm.post.createdAt < :cutoffDate")
    int resetOldPostMetrics(@Param("cutoffDate") Instant cutoffDate);
}