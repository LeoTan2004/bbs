package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.model.VerifiedInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class VerifiedInfoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerifiedInfoRepository verifiedInfoRepository;

    private User testUser1;
    private VerifiedInfo verifiedInfo1;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        entityManager.getEntityManager().createQuery("DELETE FROM VerifiedInfo").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();

        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz");
        testUser1.setNickname("Test User 1");
        testUser1.setBio("This is test user 1");
        testUser1.setProfileSlug("test-user-1");
        testUser1.setAvatarUrl("https://example.com/avatar1.jpg");
        testUser1.setRole(Role.User);
        testUser1.setStatus(Status.Active);
        testUser1 = entityManager.persistAndFlush(testUser1);

        User testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPasswordHash("$2a$10$123456789abcdefghijklmnopq");
        testUser2.setNickname("Test User 2");
        testUser2.setBio("This is test user 2");
        testUser2.setProfileSlug("test-user-2");
        testUser2.setAvatarUrl("https://example.com/avatar2.jpg");
        testUser2.setRole(Role.User);
        testUser2.setStatus(Status.Active);
        testUser2 = entityManager.persistAndFlush(testUser2);

        // Create test verified info
        verifiedInfo1 = new VerifiedInfo();
        verifiedInfo1.setUser(testUser1);
        verifiedInfo1.setFullName("张三");
        verifiedInfo1.setSid("2021001001");
        verifiedInfo1.setInstitution("湘潭大学");
        verifiedInfo1.setVisible(true);
        verifiedInfo1 = entityManager.persistAndFlush(verifiedInfo1);

        VerifiedInfo verifiedInfo2 = new VerifiedInfo();
        verifiedInfo2.setUser(testUser2);
        verifiedInfo2.setFullName("李四");
        verifiedInfo2.setSid("2021001002");
        verifiedInfo2.setInstitution("湘潭大学");
        verifiedInfo2.setVisible(false);
        entityManager.persistAndFlush(verifiedInfo2);

        // Create another user for different institution
        User testUser3 = new User();
        testUser3.setUsername("testuser3");
        testUser3.setPasswordHash("$2a$10$987654321abcdefghijklmnop");
        testUser3.setNickname("Test User 3");
        testUser3.setRole(Role.User);
        testUser3.setStatus(Status.Active);
        testUser3 = entityManager.persistAndFlush(testUser3);

        VerifiedInfo verifiedInfo3 = new VerifiedInfo();
        verifiedInfo3.setUser(testUser3);
        verifiedInfo3.setFullName("王五");
        verifiedInfo3.setSid("2021002001");
        verifiedInfo3.setInstitution("中南大学");
        verifiedInfo3.setVisible(true);
        entityManager.persistAndFlush(verifiedInfo3);

        entityManager.clear(); // Clear persistence context
    }

    @Test
    @DisplayName("Should return page of verified info when institution exists")
    void testFindByInstitution_ExistingInstitution_ShouldReturnPageOfVerifiedInfo() {
        // Given
        String institution = "湘潭大学";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<VerifiedInfo> result = verifiedInfoRepository.findByInstitution(institution, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(VerifiedInfo::getInstitution)
                .allMatch(inst -> inst.equals(institution));
        assertThat(result.getContent()).extracting(VerifiedInfo::getFullName)
                .containsExactlyInAnyOrder("张三", "李四");
    }

    @Test
    @DisplayName("Should return empty page when institution does not exist")
    void testFindByInstitution_NonExistingInstitution_ShouldReturnEmptyPage() {
        // Given
        String institution = "清华大学";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<VerifiedInfo> result = verifiedInfoRepository.findByInstitution(institution, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should respect page size when pagination is used")
    void testFindByInstitution_WithPagination_ShouldRespectPageSize() {
        // Given
        String institution = "湘潭大学";
        Pageable pageable = PageRequest.of(0, 1); // Page size of 1

        // When
        Page<VerifiedInfo> result = verifiedInfoRepository.findByInstitution(institution, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should return verified info when user exists")
    void testFindByUser_ExistingUser_ShouldReturnVerifiedInfo() {
        // When
        Optional<VerifiedInfo> result = verifiedInfoRepository.findByUser(testUser1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(result.get().getFullName()).isEqualTo("张三");
        assertThat(result.get().getSid()).isEqualTo("2021001001");
        assertThat(result.get().getInstitution()).isEqualTo("湘潭大学");
        assertThat(result.get().getVisible()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when user does not exist")
    void testFindByUser_NonExistingUser_ShouldReturnEmpty() {
        // Given
        User nonExistingUser = new User();
        nonExistingUser.setId(99999);

        // When
        Optional<VerifiedInfo> result = verifiedInfoRepository.findByUser(nonExistingUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when user has no verification")
    void testFindByUser_UserWithoutVerification_ShouldReturnEmpty() {
        // Given
        User userWithoutVerification = new User();
        userWithoutVerification.setUsername("unverifieduser");
        userWithoutVerification.setPasswordHash("$2a$10$password");
        userWithoutVerification.setRole(Role.User);
        userWithoutVerification.setStatus(Status.Active);
        userWithoutVerification = entityManager.persistAndFlush(userWithoutVerification);

        // When
        Optional<VerifiedInfo> result = verifiedInfoRepository.findByUser(userWithoutVerification);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should persist successfully when saving new verified info")
    void testSave_NewVerifiedInfo_ShouldPersistSuccessfully() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPasswordHash("$2a$10$newpassword");
        newUser.setRole(Role.User);
        newUser.setStatus(Status.Active);
        newUser = entityManager.persistAndFlush(newUser);

        VerifiedInfo newVerifiedInfo = new VerifiedInfo();
        newVerifiedInfo.setUser(newUser);
        newVerifiedInfo.setFullName("赵六");
        newVerifiedInfo.setSid("2021003001");
        newVerifiedInfo.setInstitution("北京大学");
        newVerifiedInfo.setVisible(true);

        // When
        VerifiedInfo savedVerifiedInfo = verifiedInfoRepository.save(newVerifiedInfo);

        // Then
        assertThat(savedVerifiedInfo).isNotNull();
        assertThat(savedVerifiedInfo.getId()).isNotNull();
        assertThat(savedVerifiedInfo.getFullName()).isEqualTo("赵六");
        assertThat(savedVerifiedInfo.getSid()).isEqualTo("2021003001");
        assertThat(savedVerifiedInfo.getInstitution()).isEqualTo("北京大学");
        assertThat(savedVerifiedInfo.getVisible()).isTrue();
        assertThat(savedVerifiedInfo.getVerifiedAt()).isNotNull();

        // Verify persistence
        entityManager.clear();
        VerifiedInfo foundVerifiedInfo = entityManager.find(VerifiedInfo.class, savedVerifiedInfo.getId());
        assertThat(foundVerifiedInfo).isNotNull();
        assertThat(foundVerifiedInfo.getFullName()).isEqualTo("赵六");
    }

    @Test
    @DisplayName("Should return verified info when ID exists")
    void testFindById_ExistingId_ShouldReturnVerifiedInfo() {
        // When
        Optional<VerifiedInfo> result = verifiedInfoRepository.findById(verifiedInfo1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(verifiedInfo1.getId());
        assertThat(result.get().getFullName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("Should return empty when ID does not exist")
    void testFindById_NonExistingId_ShouldReturnEmpty() {
        // When
        Optional<VerifiedInfo> result = verifiedInfoRepository.findById(99999);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should remove from database when deleting existing verified info")
    void testDelete_ExistingVerifiedInfo_ShouldRemoveFromDatabase() {
        // Given
        Integer verifiedInfoId = verifiedInfo1.getId();

        // When
        verifiedInfoRepository.delete(verifiedInfo1);
        entityManager.flush();

        // Then
        Optional<VerifiedInfo> result = verifiedInfoRepository.findById(verifiedInfoId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when ID exists")
    void testExistsById_ExistingId_ShouldReturnTrue() {
        // When
        boolean exists = verifiedInfoRepository.existsById(verifiedInfo1.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when ID does not exist")
    void testExistsById_NonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = verifiedInfoRepository.existsById(99999);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return correct count when counting all records")
    void testCount_ShouldReturnCorrectCount() {
        // When
        long count = verifiedInfoRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return all verified info when finding all records")
    void testFindAll_ShouldReturnAllVerifiedInfo() {
        // When
        Iterable<VerifiedInfo> result = verifiedInfoRepository.findAll();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(VerifiedInfo::getFullName)
                .containsExactlyInAnyOrder("张三", "李四", "王五");
    }
}