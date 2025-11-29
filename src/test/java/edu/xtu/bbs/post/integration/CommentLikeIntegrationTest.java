package edu.xtu.bbs.post.integration;

import edu.xtu.bbs.post.model.CommentLike;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.repo.CommentLikeRepository;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(CommentLikeIntegrationTestConfig.class)
@DisplayName("Comment Like Integration Tests")
class CommentLikeIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentLikeService commentLikeService;

    private User testUser1;
    private User testUser2;
    private Post testPost;
    private PostComment testComment;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setNickname("Test User 1");
        testUser1.setRole(Role.User);
        testUser1.setStatus(Status.Active);
        testUser1.setPasswordHash("hashed_password_1");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setNickname("Test User 2");
        testUser2.setRole(Role.User);
        testUser2.setStatus(Status.Active);
        testUser2.setPasswordHash("hashed_password_2");
        testUser2 = userRepository.save(testUser2);

        // Create test post
        testPost = new Post();
        testPost.setTitle("Test Post");
        testPost.setContent("This is a test post");
        testPost.setAuthor(testUser1);
        testPost = postRepository.save(testPost);

        // Create test comment
        testComment = new PostComment();
        testComment.setContent("This is a test comment");
        testComment.setUser(testUser1);
        testComment.setPost(testPost);
        testComment.setLikes(0);  // Initialize likes count
        testComment = commentRepository.save(testComment);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Database Operations Tests")
    class DatabaseOperationsTests {

        @Test
        @DisplayName("Should save and retrieve comment like successfully")
        @Transactional
        void shouldSaveAndRetrieveCommentLikeSuccessfully() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser1);
            commentLike.setCreatedAt(LocalDateTime.now());

            // When
            CommentLike saved = commentLikeRepository.save(commentLike);
            entityManager.flush();
            
            Optional<CommentLike> retrieved = commentLikeRepository.findById(saved.getId());

            // Then
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getComment().getId()).isEqualTo(testComment.getId());
            assertThat(retrieved.get().getUser().getId()).isEqualTo(testUser1.getId());
            assertThat(retrieved.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should enforce unique constraint on comment_id and user_id")
        @Transactional
        void shouldEnforceUniqueConstraintOnCommentIdAndUserId() {
            // Given
            CommentLike firstLike = new CommentLike();
            firstLike.setComment(testComment);
            firstLike.setUser(testUser1);
            firstLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(firstLike);
            entityManager.flush();

            // When & Then
            // This should work fine - different user
            CommentLike secondLike = new CommentLike();
            secondLike.setComment(testComment);
            secondLike.setUser(testUser2);
            secondLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(secondLike);
            entityManager.flush();

            // Verify both likes exist
            List<CommentLike> allLikes = commentLikeRepository.findAll();
            assertThat(allLikes).hasSize(2);
        }

        @Test
        @DisplayName("Should delete like when comment is deleted (cascade)")
        @Transactional
        void shouldDeleteLikeWhenCommentIsDeleted() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser1);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(commentLike);
            entityManager.flush();

            // Verify like exists
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId())).isTrue();

            // When - manually delete likes first to respect foreign key constraints
            commentLikeRepository.deleteByCommentIdAndUserId(testComment.getId(), testUser1.getId());
            entityManager.flush();
            commentRepository.delete(testComment);
            entityManager.flush();

            // Then
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId())).isFalse();
            assertThat(commentLikeRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should delete like when user is deleted (cascade)")
        @Transactional
        void shouldDeleteLikeWhenUserIsDeleted() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser2);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(commentLike);
            entityManager.flush();

            // Verify like exists
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId())).isTrue();

            // When - manually delete likes first to respect foreign key constraints
            commentLikeRepository.deleteByCommentIdAndUserId(testComment.getId(), testUser2.getId());
            entityManager.flush();
            userRepository.delete(testUser2);
            entityManager.flush();

            // Then
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Repository Query Methods Tests")
    class RepositoryQueryMethodsTests {

        @Test
        @DisplayName("Should find like by comment ID and user ID")
        @Transactional
        void shouldFindLikeByCommentIdAndUserId() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser1);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(commentLike);
            entityManager.flush();

            // When
            Optional<CommentLike> found = commentLikeRepository.findByCommentIdAndUserId(
                    testComment.getId(), testUser1.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getComment().getId()).isEqualTo(testComment.getId());
            assertThat(found.get().getUser().getId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("Should count likes by comment ID correctly")
        @Transactional
        void shouldCountLikesByCommentIdCorrectly() {
            // Given
            // User1 likes the comment
            CommentLike like1 = new CommentLike();
            like1.setComment(testComment);
            like1.setUser(testUser1);
            like1.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(like1);

            // User2 likes the comment
            CommentLike like2 = new CommentLike();
            like2.setComment(testComment);
            like2.setUser(testUser2);
            like2.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(like2);

            entityManager.flush();

            // When
            Long count = commentLikeRepository.countByCommentId(testComment.getId());

            // Then
            assertThat(count).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should check existence by comment ID and user ID correctly")
        @Transactional
        void shouldCheckExistenceByCommentIdAndUserIdCorrectly() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser1);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(commentLike);
            entityManager.flush();

            // When & Then
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId())).isTrue();
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId())).isFalse();
        }

        @Test
        @DisplayName("Should delete by comment ID and user ID correctly")
        @Transactional
        void shouldDeleteByCommentIdAndUserIdCorrectly() {
            // Given
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(testComment);
            commentLike.setUser(testUser1);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(commentLike);
            entityManager.flush();

            // Verify it exists
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId())).isTrue();

            // When
            commentLikeRepository.deleteByCommentIdAndUserId(testComment.getId(), testUser1.getId());
            entityManager.flush();

            // Then
            assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Service Layer Integration Tests")
    class ServiceLayerIntegrationTests {

        @Test
        @DisplayName("Should complete full like workflow through service")
        @Transactional
        void shouldCompleteFullLikeWorkflowThroughService() throws UserNotFoundException {
            // Given
            Integer userId = testUser1.getId();
            Integer commentId = testComment.getId();

            // Initially, user has not liked the comment
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isFalse();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(0L);

            // When user likes the comment
            commentLikeService.likeComment(userId, commentId);
            entityManager.flush();

            // Then
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isTrue();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(1L);

            // When user unlikes the comment
            commentLikeService.unlikeComment(userId, commentId);
            entityManager.flush();

            // Then
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isFalse();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should complete toggle workflow through service")
        @Transactional
        void shouldCompleteToggleWorkflowThroughService() throws UserNotFoundException {
            // Given
            Integer userId = testUser1.getId();
            Integer commentId = testComment.getId();

            // Initially, user has not liked the comment
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isFalse();

            // When user toggles (should like)
            Boolean result1 = commentLikeService.toggleCommentLike(userId, commentId);
            entityManager.flush();

            // Then
            assertThat(result1).isTrue();
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isTrue();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(1L);

            // When user toggles again (should unlike)
            Boolean result2 = commentLikeService.toggleCommentLike(userId, commentId);
            entityManager.flush();

            // Then
            assertThat(result2).isFalse();
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isFalse();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle multiple users liking the same comment")
        @Transactional
        void shouldHandleMultipleUsersLikingTheSameComment() throws UserNotFoundException {
            // Given
            Integer commentId = testComment.getId();

            // When both users like the comment
            commentLikeService.likeComment(testUser1.getId(), commentId);
            commentLikeService.likeComment(testUser2.getId(), commentId);
            entityManager.flush();

            // Then
            assertThat(commentLikeService.hasLikedComment(testUser1.getId(), commentId)).isTrue();
            assertThat(commentLikeService.hasLikedComment(testUser2.getId(), commentId)).isTrue();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(2L);

            // When one user unlikes
            commentLikeService.unlikeComment(testUser1.getId(), commentId);
            entityManager.flush();

            // Then
            assertThat(commentLikeService.hasLikedComment(testUser1.getId(), commentId)).isFalse();
            assertThat(commentLikeService.hasLikedComment(testUser2.getId(), commentId)).isTrue();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency Tests")
    class PerformanceAndConcurrencyTests {

        @Test
        @DisplayName("Should handle rapid like/unlike operations")
        @Transactional
        void shouldHandleRapidLikeUnlikeOperations() throws UserNotFoundException {
            // Given
            Integer userId = testUser1.getId();
            Integer commentId = testComment.getId();

            // When performing rapid operations
            for (int i = 0; i < 5; i++) {
                commentLikeService.toggleCommentLike(userId, commentId);
                entityManager.flush();
            }

            // Then - should end up with unlike state (odd number of toggles)
            assertThat(commentLikeService.hasLikedComment(userId, commentId)).isTrue();
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should maintain data consistency under load")
        @Transactional
        void shouldMaintainDataConsistencyUnderLoad() throws UserNotFoundException {
            // Given
            Integer commentId = testComment.getId();

            // When multiple operations occur
            commentLikeService.likeComment(testUser1.getId(), commentId);
            commentLikeService.likeComment(testUser2.getId(), commentId);
            entityManager.flush();

            Long initialCount = commentLikeService.getCommentLikeCount(commentId);

            commentLikeService.unlikeComment(testUser1.getId(), commentId);
            entityManager.flush();

            // Then
            assertThat(commentLikeService.getCommentLikeCount(commentId)).isEqualTo(initialCount - 1);
            
            // Database state should be consistent
            Long dbCount = commentLikeRepository.countByCommentId(commentId);
            Long serviceCount = commentLikeService.getCommentLikeCount(commentId);
            assertThat(dbCount).isEqualTo(serviceCount);
        }
    }
}