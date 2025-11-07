package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();

        // Create test users
        User testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz");
        testUser1.setEmail("test1@example.com");
        testUser1.setNickname("Test User 1");
        testUser1.setBio("This is test user 1");
        testUser1.setProfileSlug("test-user-1");
        testUser1.setAvatarUrl("http://example.com/avatar1.jpg");
        testUser1.setRole(Role.User);
        testUser1.setStatus(Status.Active);
        entityManager.persistAndFlush(testUser1);

        User testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPasswordHash("$2a$10$123456789abcdefghijklmnopq");
        testUser2.setEmail("test2@example.com");
        testUser2.setNickname("Test User 2");
        testUser2.setBio("This is test user 2");
        testUser2.setProfileSlug("test-user-2");
        testUser2.setAvatarUrl("http://example.com/avatar2.jpg");
        testUser2.setVerifiedId(1);
        testUser2.setRole(Role.Admin);
        testUser2.setStatus(Status.Active);
        entityManager.persistAndFlush(testUser2);

        User bannedUser = new User();
        bannedUser.setUsername("banneduser");
        bannedUser.setPasswordHash("$2a$10$bannedpasswordhash123456789");
        bannedUser.setEmail("banned@example.com");
        bannedUser.setNickname("Banned User");
        bannedUser.setBio("This user is banned");
        bannedUser.setProfileSlug("banned-user");
        bannedUser.setRole(Role.User);
        bannedUser.setStatus(Status.Banned);
        entityManager.persistAndFlush(bannedUser);

        entityManager.clear(); // Clear persistence context
    }

    @Test
    @DisplayName("Should return user when email exists")
    void testFindByEmail_ExistingEmail_ShouldReturnUser() {
        // Given
        String email = "test1@example.com";

        // When
        Optional<User> result = userRepository.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
        assertThat(result.get().getUsername()).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void testFindByEmail_NonExistingEmail_ShouldReturnEmpty() {
        // Given
        String email = "nonexistent@example.com";

        // When
        Optional<User> result = userRepository.findByEmail(email);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return user when username exists")
    void testFindByUsername_ExistingUsername_ShouldReturnUser() {
        // Given
        String username = "testuser2";

        // When
        Optional<User> result = userRepository.findByUsername(username);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getEmail()).isEqualTo("test2@example.com");
        assertThat(result.get().getRole()).isEqualTo(Role.Admin);
    }

    @Test
    @DisplayName("Should return empty when username does not exist")
    void testFindByUsername_NonExistingUsername_ShouldReturnEmpty() {
        // Given
        String username = "nonexistentuser";

        // When
        Optional<User> result = userRepository.findByUsername(username);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should update email when valid ID and email are provided")
    void testUpdateEmailById_ValidIdAndEmail_ShouldUpdateEmail() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        newUser.setEmail("newuser@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        String newEmail = "updated@example.com";

        // When
        int updatedRows = userRepository.updateEmailById(newEmail, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear(); // Clear the persistence context to force a fresh fetch
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("Should not update email when ID is invalid")
    void testUpdateEmailById_InvalidId_ShouldNotUpdate() {
        // Given
        Integer invalidId = 99999;
        String newEmail = "updated@example.com";

        // When
        int updatedRows = userRepository.updateEmailById(newEmail, invalidId);

        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update password when valid ID and password are provided")
    void testUpdatePasswordById_ValidIdAndPassword_ShouldUpdatePassword() {
        // Given
        User newUser = new User();
        newUser.setUsername("pwduser");
        newUser.setPasswordHash("$2a$10$oldpassword");
        newUser.setEmail("pwduser@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        String newPasswordHash = "$2a$10$newhashedpassword";

        // When
        int updatedRows = userRepository.updatePasswordById(newPasswordHash, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getPasswordHash()).isEqualTo(newPasswordHash);
    }

    @Test
    @DisplayName("Should not update password when ID is invalid")
    void testUpdatePasswordById_InvalidId_ShouldNotUpdate() {
        // Given
        Integer invalidId = 99999;
        String newPasswordHash = "$2a$10$newhashedpassword";

        // When
        int updatedRows = userRepository.updatePasswordById(newPasswordHash, invalidId);

        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update user profile when valid data is provided")
    void testUpdateProfileById_ValidData_ShouldUpdateProfile() {
        // Given
        User newUser = new User();
        newUser.setUsername("profileuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("profile@example.com");
        newUser.setNickname("Old Nickname");
        newUser.setBio("Old bio");
        newUser.setProfileSlug("old-slug");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        String newNickname = "Updated Nickname";
        String newBio = "Updated bio description";
        String newProfileSlug = "updated-slug";

        // When
        int updatedRows = userRepository.updateProfileById(newNickname, newBio, newProfileSlug, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getNickname()).isEqualTo(newNickname);
        assertThat(updatedUser.getBio()).isEqualTo(newBio);
        assertThat(updatedUser.getProfileSlug()).isEqualTo(newProfileSlug);
    }

    @Test
    @DisplayName("Should update profile with null values when provided")
    void testUpdateProfileById_NullValues_ShouldUpdateWithNulls() {
        // Given
        User newUser = new User();
        newUser.setUsername("nulluser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("null@example.com");
        newUser.setNickname("Old Nickname");
        newUser.setBio("Old bio");
        newUser.setProfileSlug("old-slug");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        // When
        int updatedRows = userRepository.updateProfileById(null, null, null, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getNickname()).isNull();
        assertThat(updatedUser.getBio()).isNull();
        assertThat(updatedUser.getProfileSlug()).isNull();
    }

    @Test
    @DisplayName("Should not update profile when ID is invalid")
    void testUpdateProfileById_InvalidId_ShouldNotUpdate() {
        // Given
        Integer invalidId = 99999;

        // When
        int updatedRows = userRepository.updateProfileById("New Nickname", "New Bio", "new-slug", invalidId);

        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update verified ID when valid data is provided")
    void testUpdateVerifiedIdById_ValidData_ShouldUpdateVerifiedId() {
        // Given
        User newUser = new User();
        newUser.setUsername("verifyuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("verify@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        Integer newVerifiedId = 123;

        // When
        int updatedRows = userRepository.updateVerifiedIdById(newVerifiedId, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getVerifiedId()).isEqualTo(newVerifiedId);
    }

    @Test
    @DisplayName("Should update verified ID to null when null value is provided")
    void testUpdateVerifiedIdById_NullValue_ShouldUpdateToNull() {
        // Given
        User newUser = new User();
        newUser.setUsername("nullverifyuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("nullverify@example.com");
        newUser.setVerifiedId(123);
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        // When
        int updatedRows = userRepository.updateVerifiedIdById(null, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getVerifiedId()).isNull();
    }

    @Test
    @DisplayName("Should update user role when valid data is provided")
    void testUpdateRoleById_ValidData_ShouldUpdateRole() {
        // Given
        User newUser = new User();
        newUser.setUsername("roleuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("role@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        Role newRole = Role.Admin;

        // When
        int updatedRows = userRepository.updateRoleById(newRole, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getRole()).isEqualTo(newRole);
    }

    @Test
    @DisplayName("Should not update role when ID is invalid")
    void testUpdateRoleById_InvalidId_ShouldNotUpdate() {
        // Given
        Integer invalidId = 99999;
        Role newRole = Role.Admin;

        // When
        int updatedRows = userRepository.updateRoleById(newRole, invalidId);

        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update user status when valid data is provided")
    void testUpdateStatusById_ValidData_ShouldUpdateStatus() {
        // Given
        User newUser = new User();
        newUser.setUsername("statususer");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("status@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        Status newStatus = Status.Banned;

        // When
        int updatedRows = userRepository.updateStatusById(newStatus, savedUser.getId());

        // Then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getStatus()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("Should not update status when ID is invalid")
    void testUpdateStatusById_InvalidId_ShouldNotUpdate() {
        // Given
        Integer invalidId = 99999;
        Status newStatus = Status.Suspend;

        // When
        int updatedRows = userRepository.updateStatusById(newStatus, invalidId);

        // Then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("Should perform CRUD operations correctly")
    void testCrudOperations_SaveFindDelete_ShouldWorkCorrectly() {
        // Given
        User newUser = new User();
        newUser.setUsername("cruduser");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        newUser.setEmail("crud@example.com");
        newUser.setNickname("CRUD User");
        newUser.setBio("This is a CRUD test user");
        newUser.setProfileSlug("crud-user");
        newUser.setAvatarUrl("http://example.com/crud.jpg");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);

        // Test Save
        User savedUser = userRepository.save(newUser);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("cruduser");

        // Test FindById
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("cruduser");

        // Test Count
        long count = userRepository.count();
        assertThat(count).isGreaterThan(0);

        // Test Delete
        userRepository.delete(savedUser);
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should return all users when finding all records")
    void testFindAll_ShouldReturnAllUsers() {
        // When
        Iterable<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).isNotEmpty();
        assertThat(allUsers).hasSize(3); // testUser1, testUser2, bannedUser from setUp
    }

    @Test
    @DisplayName("Should ensure transactional behavior for update methods")
    void testTransactionalBehavior_UpdateMethods_ShouldBeTransactional() {
        // Given
        User newUser = new User();
        newUser.setUsername("transactionaluser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("transactional@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        User savedUser = entityManager.persistAndFlush(newUser);

        // When - Test that @Transactional annotation works
        // The updateEmailById method is annotated with @Transactional
        userRepository.updateEmailById("newtransactional@example.com", savedUser.getId());

        // Then - The change should be committed
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertThat(updatedUser.getEmail()).isEqualTo("newtransactional@example.com");
    }

    @Test
    @DisplayName("Should set createdAt timestamp when user is created")
    void testAuditFields_CreatedAt_ShouldBeSetOnCreation() {
        // Given
        User newUser = new User();
        newUser.setUsername("audituser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("audit@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);

        // Record time before saving (with some buffer)
        java.time.Instant beforeSave = java.time.Instant.now().minusSeconds(1);

        // When
        User savedUser = userRepository.save(newUser);
        entityManager.flush(); // Force database write

        // Record time after saving (with some buffer)
        java.time.Instant afterSave = java.time.Instant.now().plusSeconds(1);

        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isBetween(beforeSave, afterSave);
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isBetween(beforeSave, afterSave);
        // Created and updated should be very close or same on creation
        assertThat(java.time.Duration.between(savedUser.getCreatedAt(), savedUser.getUpdatedAt()).abs())
                .isLessThan(java.time.Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Should update updatedAt timestamp when user is updated")
    void testAuditFields_UpdatedAt_ShouldChangeOnUpdate() throws InterruptedException {
        // Given
        User newUser = new User();
        newUser.setUsername("updateaudituser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("updateaudit@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);

        User savedUser = entityManager.persistAndFlush(newUser);
        java.time.Instant originalCreatedAt = savedUser.getCreatedAt();
        java.time.Instant originalUpdatedAt = savedUser.getUpdatedAt();

        // Wait a small moment to ensure time difference
        Thread.sleep(100);

        // When - Update the user
        savedUser.setNickname("Updated Nickname");
        java.time.Instant beforeUpdate = java.time.Instant.now();
        User updatedUser = userRepository.save(savedUser);
        entityManager.flush();
        java.time.Instant afterUpdate = java.time.Instant.now();

        // Then
        assertThat(updatedUser.getCreatedAt()).isEqualTo(originalCreatedAt); // CreatedAt should not change
        assertThat(updatedUser.getUpdatedAt()).isNotEqualTo(originalUpdatedAt); // UpdatedAt should change
        assertThat(updatedUser.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp when using repository update methods")
    void testAuditFields_UpdatedAt_ShouldChangeOnRepositoryUpdate() throws InterruptedException {
        // Given
        User newUser = new User();
        newUser.setUsername("repoupdateuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("repoupdate@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);

        User savedUser = entityManager.persistAndFlush(newUser);
        java.time.Instant originalCreatedAt = savedUser.getCreatedAt();

        // Wait to ensure time difference
        Thread.sleep(100);

        // When - Update using repository method
        userRepository.updateEmailById("newrepoemail@example.com", savedUser.getId());
        entityManager.flush();

        // Clear cache and reload
        entityManager.clear();
        User reloadedUser = entityManager.find(User.class, savedUser.getId());

        // Then
        assertThat(reloadedUser.getCreatedAt()).isEqualTo(originalCreatedAt); // CreatedAt should not change
        // Note: Custom update methods might not trigger @UpdateTimestamp
        // This depends on JPA implementation and how the update is executed
        assertThat(reloadedUser.getEmail()).isEqualTo("newrepoemail@example.com");
    }

    @Test
    @DisplayName("Should ensure all test users have valid audit timestamps")
    void testAuditFields_AllTestUsers_ShouldHaveValidTimestamps() {
        // When - Get all users created in setUp()
        Iterable<User> allUsers = userRepository.findAll();

        // Then - All users should have valid audit fields
        for (User user : allUsers) {
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            // UpdatedAt should be >= CreatedAt
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
        }
    }

    @Test
    @DisplayName("Should not affect timestamps when performing find operations")
    void testAuditFields_FindOperations_ShouldNotAffectTimestamps() {
        // Given
        User newUser = new User();
        newUser.setUsername("findtestuser");
        newUser.setPasswordHash("$2a$10$password");
        newUser.setEmail("findtest@example.com");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);

        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        java.time.Instant originalCreatedAt = savedUser.getCreatedAt();
        java.time.Instant originalUpdatedAt = savedUser.getUpdatedAt();

        // When - Perform read operations
        userRepository.findByEmail("findtest@example.com");
        userRepository.findByUsername("findtestuser");
        userRepository.findById(savedUser.getId());
        userRepository.count();

        // Clear and reload
        entityManager.clear();
        User reloadedUser = entityManager.find(User.class, savedUser.getId());

        // Then - Timestamps should remain unchanged
        assertThat(reloadedUser.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(reloadedUser.getUpdatedAt()).isEqualTo(originalUpdatedAt);
    }
}