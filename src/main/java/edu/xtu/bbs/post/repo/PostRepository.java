package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    // Basic query methods using Spring Data JPA naming convention

    /**
     * Find all posts by a specific author
     *
     * @param authorId The ID of the author
     * @param pageable Pagination information
     * @return Paginated list of posts by the author
     */
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Integer authorId, Pageable pageable);

    /**
     * Find posts by category with pagination
     *
     * @param category The category name
     * @param pageable Pagination information
     * @return Paginated list of posts in the category
     */
    Page<Post> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    /**
     * Find posts by status
     *
     * @param status   The post status
     * @param pageable Pagination information
     * @return Paginated list of posts with the specified status
     */
    Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    /**
     * Find posts that are not possibly sensitive
     *
     * @param pageable Pagination information
     * @return Paginated list of non-sensitive posts
     */
    Page<Post> findByPossiblySensitiveFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find posts created after a specific time
     *
     * @param createdAt The timestamp threshold
     * @param pageable  Pagination information
     * @return Paginated list of posts created after the specified time
     */
    Page<Post> findByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt, Pageable pageable);

    /**
     * Search posts by title containing a keyword (case-insensitive)
     *
     * @param keyword  The search keyword
     * @param pageable Pagination information
     * @return Paginated list of posts with titles containing the keyword
     */
    Page<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    // Custom JPQL queries for more complex operations

    /**
     * Find posts with highest engagement (likes + favorites + views) in a time range
     *
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @param pageable  Pagination information
     * @return Paginated list of posts sorted by engagement
     */
    @Query("SELECT p FROM Post p " +
            "JOIN p.publicMatrics pm " +
            "WHERE p.createdAt BETWEEN :startTime AND :endTime " +
            "AND p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "ORDER BY (pm.likes + pm.favorites + pm.views) DESC")
    Page<Post> findMostEngagedPostsInTimeRange(@Param("startTime") Instant startTime,
                                               @Param("endTime") Instant endTime,
                                               Pageable pageable);

    /**
     * Find trending posts based on recent likes and views
     *
     * @param hoursBack Number of hours to look back for trending calculation
     * @param pageable  Pagination information
     * @return Paginated list of trending posts
     */
    @Query("SELECT p FROM Post p " +
            "JOIN p.publicMatrics pm " +
            "WHERE p.createdAt >= :sinceTime " +
            "AND p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "AND p.possiblySensitive = false " +
            "ORDER BY (pm.likes * 2 + pm.views) DESC")
    Page<Post> findTrendingPosts(@Param("sinceTime") Instant sinceTime, Pageable pageable);

    /**
     * Search posts by title or content
     *
     * @param searchTerm The search term
     * @param pageable   Pagination information
     * @return Paginated list of posts matching the search term
     */
    @Query("SELECT p FROM Post p " +
            "WHERE p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR p.content LIKE CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> searchPostsByContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find posts by multiple categories
     *
     * @param categories List of category names
     * @param pageable   Pagination information
     * @return Paginated list of posts in any of the specified categories
     */
    @Query("SELECT p FROM Post p " +
            "WHERE p.category IN :categories " +
            "AND p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findByMultipleCategories(@Param("categories") List<String> categories, Pageable pageable);

    /**
     * Find posts by author with minimum engagement threshold
     *
     * @param authorId The author's ID
     * @param minLikes Minimum number of likes
     * @param minViews Minimum number of views
     * @param pageable Pagination information
     * @return Paginated list of popular posts by the author
     */
    @Query("SELECT p FROM Post p " +
            "JOIN p.publicMatrics pm " +
            "WHERE p.author.id = :authorId " +
            "AND pm.likes >= :minLikes " +
            "AND pm.views >= :minViews " +
            "ORDER BY pm.likes DESC, pm.views DESC")
    Page<Post> findPopularPostsByAuthor(@Param("authorId") Integer authorId,
                                        @Param("minLikes") Integer minLikes,
                                        @Param("minViews") Integer minViews,
                                        Pageable pageable);

    /**
     * Get posts recommended for a user based on their activity
     * Excludes posts they've already liked or favorited
     *
     * @param userId     The user's ID
     * @param categories Preferred categories
     * @param pageable   Pagination information
     * @return Paginated list of recommended posts
     */
    @Query("SELECT p FROM Post p " +
            "JOIN p.publicMatrics pm " +
            "WHERE p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "AND p.possiblySensitive = false " +
            "AND p.category IN :categories " +
            "AND p.author.id != :userId " +
            "AND p.id NOT IN (SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId) " +
            "AND p.id NOT IN (SELECT pf.post.id FROM PostFavorite pf WHERE pf.user.id = :userId) " +
            "ORDER BY pm.likes DESC, pm.views DESC")
    Page<Post> findRecommendedPosts(@Param("userId") Integer userId,
                                    @Param("categories") List<String> categories,
                                    Pageable pageable);

    // Statistical and count queries

    /**
     * Count posts by author in a time range
     *
     * @param authorId  The author's ID
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @return Number of posts created by the author in the time range
     */
    @Query("SELECT COUNT(p) FROM Post p " +
            "WHERE p.author.id = :authorId " +
            "AND p.createdAt BETWEEN :startTime AND :endTime")
    Long countPostsByAuthorInTimeRange(@Param("authorId") Integer authorId,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime);

    /**
     * Count posts by category and status
     *
     * @param category The category name
     * @param status   The post status
     * @return Number of posts in the category with the specified status
     */
    Long countByCategoryAndStatus(String category, PostStatus status);

    /**
     * Find all distinct categories that have published posts
     *
     * @return List of category names with published posts
     */
    @Query("SELECT DISTINCT p.category FROM Post p " +
            "WHERE p.category IS NOT NULL " +
            "AND p.status = edu.xtu.bbs.post.model.PostStatus.PUBLISHED " +
            "ORDER BY p.category")
    List<String> findAllActiveCategories();

    /**
     * Check if a user has already posted in the last specified minutes (rate limiting)
     *
     * @param authorId  The author's ID
     * @param sinceTime The time threshold
     * @return True if the user has posted recently
     */
    @Query("SELECT COUNT(p) > 0 FROM Post p " +
            "WHERE p.author.id = :authorId " +
            "AND p.createdAt >= :sinceTime")
    boolean hasUserPostedRecently(@Param("authorId") Integer authorId, @Param("sinceTime") Instant sinceTime);

    /**
     * Find posts that need moderation (reported or flagged)
     *
     * @param pageable Pagination information
     * @return Paginated list of posts requiring moderation
     */
    @Query("SELECT p FROM Post p " +
            "WHERE p.status = edu.xtu.bbs.post.model.PostStatus.UNDER_REVIEW " +
            "OR p.possiblySensitive = true " +
            "ORDER BY p.createdAt ASC")
    Page<Post> findPostsNeedingModeration(Pageable pageable);
}