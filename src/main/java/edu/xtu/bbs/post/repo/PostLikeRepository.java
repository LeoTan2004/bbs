package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
}