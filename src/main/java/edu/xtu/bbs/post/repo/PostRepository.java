package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    /**
     * Find posts by author and status
     */
    Page<Post> findByAuthorIdAndStatus(Integer authorId, PostStatus status, Pageable pageable);
    
    /**
     * Find posts by author and status (for single result)
     */
    List<Post> findByAuthorIdAndStatus(Integer authorId, PostStatus status);
    
    /**
     * Find post by id and status
     */
    Optional<Post> findByIdAndStatus(Integer id, PostStatus status);
    
    /**
     * Find posts by status
     */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    
    /**
     * Find posts by status and category
     */
    Page<Post> findByStatusAndCategory(PostStatus status, String category, Pageable pageable);
        /**
         * Search posts by status and keyword in title or content
         */
        @Query("""
                SELECT p FROM Post p
                WHERE p.status = :status
                    AND (
                        LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                """)
        Page<Post> searchByStatusAndKeyword(@Param("status") PostStatus status,
                                                                                @Param("keyword") String keyword,
                                                                                Pageable pageable);
    
    /**
     * Find posts by multiple post IDs
     */
    @Query("SELECT p FROM Post p WHERE p.id IN :postIds AND p.status = :status")
    List<Post> findByIdsAndStatus(@Param("postIds") List<Integer> postIds, @Param("status") PostStatus status);
}