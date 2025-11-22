package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.PublicMatric;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicMatricRepository extends CrudRepository<PublicMatric, Integer> {
}