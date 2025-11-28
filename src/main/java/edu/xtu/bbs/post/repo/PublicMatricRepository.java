package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PublicMatric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicMatricRepository extends JpaRepository<PublicMatric, Integer> {

}