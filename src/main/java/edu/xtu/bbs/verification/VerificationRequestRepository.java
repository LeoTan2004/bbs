package edu.xtu.bbs.verification;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends CrudRepository<VerificationRequest, Integer> {
    Optional<VerificationRequest> findByPrincipleAndToken(String principle, String token);
}