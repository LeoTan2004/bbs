package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PublicMatric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicMatricRepository extends JpaRepository<PublicMatric, Integer> {

    /**
     * Find public metric by post id
     */
    Optional<PublicMatric> findByPostId(Integer postId);
    
    /**
     * Delete public metric by post id
     */
    void deleteByPostId(Integer postId);
}