package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserBinderRepository Tests")
class UserBinderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserBinderRepository userBinderRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private UserBinder testUserBinder1;
    private UserBinder testUserBinder2;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        entityManager.getEntityManager().createQuery("DELETE FROM UserBinder").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();

        // Create test users first
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz");
        testUser1.setNickname("Test User 1");
        testUser1.setBio("This is test user 1");
        testUser1.setProfileSlug("test-user-1");
        testUser1.setAvatarUrl("http://example.com/avatar1.jpg");
        testUser1.setRole(Role.User);
        testUser1.setStatus(Status.Active);
        entityManager.persistAndFlush(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPasswordHash("$2a$10$123456789abcdefghijklmnopq");
        testUser2.setNickname("Test User 2");
        testUser2.setBio("This is test user 2");
        testUser2.setProfileSlug("test-user-2");
        testUser2.setAvatarUrl("http://example.com/avatar2.jpg");
        testUser2.setRole(Role.User);
        testUser2.setStatus(Status.Active);
        entityManager.persistAndFlush(testUser2);

        testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setPasswordHash("$2a$10$987654321abcdefghijklmnop");
        testUser3.setNickname("Test User 3");
        testUser3.setBio("This is test user 3");
        testUser3.setProfileSlug("test-user-3");
        testUser3.setAvatarUrl("http://example.com/avatar3.jpg");
        testUser3.setRole(Role.User);
        testUser3.setStatus(Status.Active);
        entityManager.persistAndFlush(testUser3);

        // Create test user binders - only one binder per user due to unique constraint
        testUserBinder1 = new UserBinder();
        testUserBinder1.setUserId(testUser1.getId());
        testUserBinder1.setBindType(BindType.EMAIL);
        testUserBinder1.setIdentifier("user1@example.com");
        entityManager.persistAndFlush(testUserBinder1);

        testUserBinder2 = new UserBinder();
        testUserBinder2.setUserId(testUser2.getId());
        testUserBinder2.setBindType(BindType.EMAIL);
        testUserBinder2.setIdentifier("user2@example.com");
        entityManager.persistAndFlush(testUserBinder2);

        entityManager.clear(); // Clear persistence context
    }

    @Test
    @DisplayName("Should find all user binders by user ID")
    void findByUserId_ShouldReturnAllBindersForUser() {
        // When
        List<UserBinder> userBinders = userBinderRepository.findByUserId(testUser1.getId());

        // Then
        assertThat(userBinders).hasSize(1);
        assertThat(userBinders).extracting(UserBinder::getUserId).containsOnly(testUser1.getId());
        assertThat(userBinders).extracting(UserBinder::getBindType).containsExactly(BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return empty list when user has no binders")
    void findByUserId_WhenUserHasNoBinders_ShouldReturnEmptyList() {
        // When
        List<UserBinder> userBinders = userBinderRepository.findByUserId(999);

        // Then
        assertThat(userBinders).isEmpty();
    }

    @Test
    @DisplayName("Should find user binder by user ID and bind type")
    void findByUserIdAndBindType_ShouldReturnCorrectBinder() {
        // When
        Optional<UserBinder> userBinder = userBinderRepository.findByUserIdAndBindType(testUser1.getId(), BindType.EMAIL);

        // Then
        assertThat(userBinder).isPresent();
        assertThat(userBinder.get().getUserId()).isEqualTo(testUser1.getId());
        assertThat(userBinder.get().getBindType()).isEqualTo(BindType.EMAIL);
        assertThat(userBinder.get().getIdentifier()).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("Should return empty when user binder not found by user ID and bind type")
    void findByUserIdAndBindType_WhenNotFound_ShouldReturnEmpty() {
        // When
        Optional<UserBinder> userBinder = userBinderRepository.findByUserIdAndBindType(999, BindType.EMAIL);

        // Then
        assertThat(userBinder).isEmpty();
    }

    @Test
    @DisplayName("Should find user binder by identifier and bind type")
    void findByIdentifierAndBindType_ShouldReturnCorrectBinder() {
        // When
        Optional<UserBinder> userBinder = userBinderRepository.findByIdentifierAndBindType("user1@example.com", BindType.EMAIL);

        // Then
        assertThat(userBinder).isPresent();
        assertThat(userBinder.get().getUserId()).isEqualTo(testUser1.getId());
        assertThat(userBinder.get().getBindType()).isEqualTo(BindType.EMAIL);
        assertThat(userBinder.get().getIdentifier()).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("Should return empty when user binder not found by identifier and bind type")
    void findByIdentifierAndBindType_WhenNotFound_ShouldReturnEmpty() {
        // When
        Optional<UserBinder> userBinder = userBinderRepository.findByIdentifierAndBindType("nonexistent@example.com", BindType.EMAIL);

        // Then
        assertThat(userBinder).isEmpty();
    }

    @Test
    @DisplayName("Should return true when identifier and bind type combination exists")
    void existsByIdentifierAndBindType_WhenExists_ShouldReturnTrue() {
        // When
        boolean exists = userBinderRepository.existsByIdentifierAndBindType("user1@example.com", BindType.EMAIL);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when identifier and bind type combination does not exist")
    void existsByIdentifierAndBindType_WhenNotExists_ShouldReturnFalse() {
        // When
        boolean exists = userBinderRepository.existsByIdentifierAndBindType("nonexistent@example.com", BindType.EMAIL);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true when user has bound the specified type")
    void existsByUserIdAndBindType_WhenExists_ShouldReturnTrue() {
        // When
        boolean exists = userBinderRepository.existsByUserIdAndBindType(testUser1.getId(), BindType.EMAIL);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user has not bound the specified type")
    void existsByUserIdAndBindType_WhenNotExists_ShouldReturnFalse() {
        // When
        boolean exists = userBinderRepository.existsByUserIdAndBindType(testUser2.getId(), BindType.WECHAT);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should delete user binder by user ID and bind type")
    void deleteByUserIdAndBindType_ShouldDeleteCorrectBinder() {
        // Given
        assertThat(userBinderRepository.existsByUserIdAndBindType(testUser1.getId(), BindType.EMAIL)).isTrue();

        // When
        int deletedCount = userBinderRepository.deleteByUserIdAndBindType(
                testUser1.getId() != null ? testUser1.getId() : 0, BindType.EMAIL);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(userBinderRepository.existsByUserIdAndBindType(testUser1.getId(), BindType.EMAIL)).isFalse();
        // Other binders should still exist
        assertThat(userBinderRepository.existsByUserIdAndBindType(testUser2.getId(), BindType.EMAIL)).isTrue();
    }

    @Test
    @DisplayName("Should return zero when no binder to delete")
    void deleteByUserIdAndBindType_WhenNotExists_ShouldReturnZero() {
        // When
        int deletedCount = userBinderRepository.deleteByUserIdAndBindType(999, BindType.EMAIL);

        // Then
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update user binder identifier by user ID and bind type")
    void updateIdentifierByUserIdAndBindType_ShouldUpdateCorrectBinder() {
        // Given
        String newIdentifier = "newemail@example.com";

        // When
        int updatedCount = userBinderRepository.updateIdentifierByUserIdAndBindType(
                newIdentifier, testUser1.getId() != null ? testUser1.getId() : 0, BindType.EMAIL);

        // Then
        assertThat(updatedCount).isEqualTo(1);

        Optional<UserBinder> updatedBinder = userBinderRepository.findByUserIdAndBindType(testUser1.getId(), BindType.EMAIL);
        assertThat(updatedBinder).isPresent();
        assertThat(updatedBinder.get().getIdentifier()).isEqualTo(newIdentifier);

        // Other user's binder should remain unchanged
        Optional<UserBinder> otherBinder = userBinderRepository.findByUserIdAndBindType(testUser2.getId(), BindType.EMAIL);
        assertThat(otherBinder).isPresent();
        assertThat(otherBinder.get().getIdentifier()).isEqualTo("user2@example.com");
    }

    @Test
    @DisplayName("Should return zero when no binder to update")
    void updateIdentifierByUserIdAndBindType_WhenNotExists_ShouldReturnZero() {
        // When
        int updatedCount = userBinderRepository.updateIdentifierByUserIdAndBindType("new@example.com", 999, BindType.EMAIL);

        // Then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null user ID gracefully in exists check")
    void existsByUserIdAndBindType_WithNullUserId_ShouldReturnFalse() {
        // When
        boolean exists = userBinderRepository.existsByUserIdAndBindType(null, BindType.EMAIL);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should handle null identifier gracefully in exists check")
    void existsByIdentifierAndBindType_WithNullIdentifier_ShouldReturnFalse() {
        // When
        boolean exists = userBinderRepository.existsByIdentifierAndBindType(null, BindType.EMAIL);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should save and retrieve user binder correctly")
    void save_ShouldPersistUserBinder() {
        // Given
        UserBinder newBinder = new UserBinder();
        newBinder.setUserId(testUser3.getId());
        newBinder.setBindType(BindType.EMAIL);
        newBinder.setIdentifier("user3@example.com");

        // When
        UserBinder savedBinder = userBinderRepository.save(newBinder);

        // Then
        assertThat(savedBinder.getId()).isNotNull();
        assertThat(savedBinder.getUserId()).isEqualTo(testUser3.getId());
        assertThat(savedBinder.getBindType()).isEqualTo(BindType.EMAIL);
        assertThat(savedBinder.getIdentifier()).isEqualTo("user3@example.com");
        assertThat(savedBinder.getCreatedAt()).isNotNull();
        assertThat(savedBinder.getUpdatedAt()).isNotNull();

        // Verify it exists in database
        assertThat(userBinderRepository.existsByUserIdAndBindType(testUser3.getId(), BindType.EMAIL)).isTrue();
    }
}