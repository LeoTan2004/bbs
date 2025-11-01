package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.model.VerifiedInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerifiedInfoRepository extends CrudRepository<VerifiedInfo, Integer> {
    Page<VerifiedInfo> findByInstitution(String institution, Pageable pageable);

    Optional<VerifiedInfo> findByUser(User user);
}