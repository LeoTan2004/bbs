package edu.xtu.bbs.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerifiedInfoRepository extends CrudRepository<VerifiedInfo, Integer> {

}