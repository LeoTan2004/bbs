package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.EmailVerificationRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRequestRepository extends CrudRepository<EmailVerificationRequest, Integer> {
    Optional<EmailVerificationRequest> findByEmailAndToken(String email, String token);
}