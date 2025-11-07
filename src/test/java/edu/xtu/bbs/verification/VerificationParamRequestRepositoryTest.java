package edu.xtu.bbs.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class VerificationParamRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerificationRequestRepository verificationRequestRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        entityManager.getEntityManager().createQuery("DELETE FROM VerificationRequest").executeUpdate();

        // Create test email verification requests
        VerificationRequest testRequest1 = new VerificationRequest();
        testRequest1.setPrinciple("user1@example.com");
        testRequest1.setScope("registration");
        testRequest1.setToken("token123456");
        testRequest1.setCredential("credential123");
        testRequest1.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        testRequest1.setStatus(VerificationStatus.Pending);
        // Manually set requestedAt since @CreationTimestamp might not work in tests
        testRequest1.setRequestedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        entityManager.persistAndFlush(testRequest1);

        VerificationRequest testRequest2 = new VerificationRequest();
        testRequest2.setPrinciple("user2@example.com");
        testRequest2.setScope("password_reset");
        testRequest2.setToken("token789012");
        testRequest2.setCredential("credential456");
        testRequest2.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        testRequest2.setStatus(VerificationStatus.Pending);
        testRequest2.setRequestedAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        entityManager.persistAndFlush(testRequest2);

        VerificationRequest testRequest3 = new VerificationRequest();
        testRequest3.setPrinciple("user1@example.com");
        testRequest3.setScope("email_change");
        testRequest3.setToken("token345678");
        testRequest3.setCredential("credential789");
        testRequest3.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        testRequest3.setStatus(VerificationStatus.Verified);
        testRequest3.setRequestedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        entityManager.persistAndFlush(testRequest3);

        entityManager.clear();
    }

    @Test
    @DisplayName("Should return verification request when valid email and token are provided")
    void findByEmailAndToken_WhenValidEmailAndTokenProvided_ShouldReturnRequest() {
        // Given
        String email = "user1@example.com";
        String token = "token123456";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPrinciple()).isEqualTo(email);
        assertThat(result.get().getToken()).isEqualTo(token);
        assertThat(result.get().getScope()).isEqualTo("registration");
        assertThat(result.get().getCredential()).isEqualTo("credential123");
        assertThat(result.get().getStatus()).isEqualTo(VerificationStatus.Pending);
    }

    @Test
    @DisplayName("Should return correct verification request for second user")
    void findByEmailAndToken_WhenValidEmailAndTokenProvidedForSecondUser_ShouldReturnCorrectRequest() {
        // Given
        String email = "user2@example.com";
        String token = "token789012";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPrinciple()).isEqualTo(email);
        assertThat(result.get().getToken()).isEqualTo(token);
        assertThat(result.get().getScope()).isEqualTo("password_reset");
        assertThat(result.get().getCredential()).isEqualTo("credential456");
        assertThat(result.get().getStatus()).isEqualTo(VerificationStatus.Pending);
    }

    @Test
    @DisplayName("Should return verified request when verification is already completed")
    void findByEmailAndToken_WhenValidEmailAndTokenProvidedForVerifiedRequest_ShouldReturnVerifiedRequest() {
        // Given
        String email = "user1@example.com";
        String token = "token345678";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPrinciple()).isEqualTo(email);
        assertThat(result.get().getToken()).isEqualTo(token);
        assertThat(result.get().getScope()).isEqualTo("email_change");
        assertThat(result.get().getCredential()).isEqualTo("credential789");
        assertThat(result.get().getStatus()).isEqualTo(VerificationStatus.Verified);
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmailAndToken_WhenNonExistentEmailProvided_ShouldReturnEmpty() {
        // Given
        String email = "nonexistent@example.com";
        String token = "token123456";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token does not exist")
    void findByEmailAndToken_WhenNonExistentTokenProvided_ShouldReturnEmpty() {
        // Given
        String email = "user1@example.com";
        String token = "nonexistenttoken";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when email is valid but token belongs to another user")
    void findByEmailAndToken_WhenValidEmailButWrongToken_ShouldReturnEmpty() {
        // Given
        String email = "user1@example.com";
        String token = "token789012"; // This token belongs to user2

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token is valid but email is wrong")
    void findByEmailAndToken_WhenValidTokenButWrongEmail_ShouldReturnEmpty() {
        // Given
        String email = "user2@example.com";
        String token = "token123456"; // This token belongs to user1

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when email is null")
    void findByEmailAndToken_WhenNullEmailProvided_ShouldReturnEmpty() {
        // Given
        String email = null;
        String token = "token123456";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token is null")
    void findByEmailAndToken_WhenNullTokenProvided_ShouldReturnEmpty() {
        // Given
        String email = "user1@example.com";
        String token = null;

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when email is empty string")
    void findByEmailAndToken_WhenEmptyEmailProvided_ShouldReturnEmpty() {
        // Given
        String email = "";
        String token = "token123456";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token is empty string")
    void findByEmailAndToken_WhenEmptyTokenProvided_ShouldReturnEmpty() {
        // Given
        String email = "user1@example.com";
        String token = "";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when email has different case (case-sensitive matching)")
    void findByEmailAndToken_WhenCaseInsensitiveEmailProvided_ShouldReturnEmptyForExactMatch() {
        // Given - Spring Data JPA is typically case-sensitive for string matching
        String email = "USER1@EXAMPLE.COM"; // Different case
        String token = "token123456";

        // When
        Optional<VerificationRequest> result = verificationRequestRepository.findByPrincipleAndToken(email, token);

        // Then
        assertThat(result).isEmpty(); // Should be empty as exact match is required
    }

    @Test
    @DisplayName("Should persist new verification request successfully")
    void save_WhenValidEmailVerificationRequestProvided_ShouldPersistSuccessfully() {
        // Given
        VerificationRequest newRequest = new VerificationRequest();
        newRequest.setPrinciple("newuser@example.com");
        newRequest.setScope("registration");
        newRequest.setToken("newtoken123");
        newRequest.setCredential("newcredential");
        newRequest.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        newRequest.setStatus(VerificationStatus.Pending);
        newRequest.setRequestedAt(Instant.now()); // Set requestedAt manually

        // When
        VerificationRequest savedRequest = verificationRequestRepository.save(newRequest);

        // Then
        assertThat(savedRequest).isNotNull();
        assertThat(savedRequest.getId()).isNotNull();
        assertThat(savedRequest.getPrinciple()).isEqualTo("newuser@example.com");
        assertThat(savedRequest.getToken()).isEqualTo("newtoken123");
        assertThat(savedRequest.getRequestedAt()).isNotNull(); // Should be set

        // Verify it can be found
        Optional<VerificationRequest> foundRequest = verificationRequestRepository.findByPrincipleAndToken("newuser@example.com", "newtoken123");
        assertThat(foundRequest).isPresent();
        assertThat(foundRequest.get().getId()).isEqualTo(savedRequest.getId());
    }

    @Test
    @DisplayName("Should find verification request by ID")
    void findById_WhenValidIdProvided_ShouldReturnRequest() {
        // Given - Find a request to get its ID
        Optional<VerificationRequest> existingRequest = verificationRequestRepository.findByPrincipleAndToken("user1@example.com", "token123456");
        assertThat(existingRequest).isPresent();

        // Extract ID safely
        VerificationRequest request = existingRequest.get();
        Integer id = request.getId();
        assertThat(id).isNotNull();

        // When - Use explicit cast to satisfy null safety
        @SuppressWarnings("null")
        Optional<VerificationRequest> result = verificationRequestRepository.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getPrinciple()).isEqualTo("user1@example.com");
        assertThat(result.get().getToken()).isEqualTo("token123456");
    }

    @Test
    @DisplayName("Should successfully remove verification request from database")
    void delete_WhenValidRequestProvided_ShouldRemoveFromDatabase() {
        // Given
        String email = "user1@example.com";
        String token = "token123456";

        // Verify it exists first
        Optional<VerificationRequest> beforeDelete = verificationRequestRepository.findByPrincipleAndToken(email, token);
        assertThat(beforeDelete).isPresent();
        long countBefore = verificationRequestRepository.count();

        // When - Create and save a new request specifically for deletion test
        VerificationRequest requestToDelete = new VerificationRequest();
        requestToDelete.setPrinciple("delete@example.com");
        requestToDelete.setScope("test");
        requestToDelete.setToken("deletetoken");
        requestToDelete.setCredential("deletecred");
        requestToDelete.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        requestToDelete.setStatus(VerificationStatus.Pending);
        requestToDelete.setRequestedAt(Instant.now()); // Set requestedAt manually
        VerificationRequest saved = verificationRequestRepository.save(requestToDelete);
        entityManager.flush();

        // Verify it was saved
        assertThat(verificationRequestRepository.count()).isEqualTo(countBefore + 1);

        // Delete it
        entityManager.remove(entityManager.find(VerificationRequest.class, saved.getId()));
        entityManager.flush();

        // Then
        assertThat(verificationRequestRepository.count()).isEqualTo(countBefore);
        Optional<VerificationRequest> afterDelete = verificationRequestRepository.findByPrincipleAndToken("delete@example.com", "deletetoken");
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("Should return correct count of verification records")
    void count_ShouldReturnCorrectNumberOfRecords() {
        // When
        long count = verificationRequestRepository.count();

        // Then
        assertThat(count).isEqualTo(3); // We have 3 test records
    }
}