package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Integer> {
}