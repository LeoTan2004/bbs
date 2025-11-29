package edu.xtu.bbs.post.repo;

import edu.xtu.bbs.post.model.CommentLike;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentLikeRepository Tests")
class CommentLikeRepositoryTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    private User testUser;
    private PostComment testComment;
    private CommentLike testCommentLike;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");

        testComment = new PostComment();
        testComment.setId(100);

        testCommentLike = new CommentLike();
        testCommentLike.setId(1);
        testCommentLike.setUser(testUser);
        testCommentLike.setComment(testComment);
    }

    @Test
    @DisplayName("Should check if user has liked comment - returns true when like exists")
    void shouldCheckIfUserHasLikedComment_ReturnsTrueWhenLikeExists() {
        // Given
        Integer commentId = 100;
        Integer userId = 1;
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);

        // When
        boolean result = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        // Then
        assertThat(result).isTrue();
        verify(commentLikeRepository).existsByCommentIdAndUserId(commentId, userId);
    }

    @Test
    @DisplayName("Should check if user has liked comment - returns false when like does not exist")
    void shouldCheckIfUserHasLikedComment_ReturnsFalseWhenLikeDoesNotExist() {
        // Given
        Integer commentId = 100;
        Integer userId = 1;
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);

        // When
        boolean result = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        // Then
        assertThat(result).isFalse();
        verify(commentLikeRepository).existsByCommentIdAndUserId(commentId, userId);
    }

    @Test
    @DisplayName("Should count likes for comment - returns correct count")
    void shouldCountLikesForComment_ReturnsCorrectCount() {
        // Given
        Integer commentId = 100;
        Long expectedCount = 5L;
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(expectedCount);

        // When
        Long result = commentLikeRepository.countByCommentId(commentId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(commentLikeRepository).countByCommentId(commentId);
    }

    @Test
    @DisplayName("Should count likes for comment - returns zero when no likes")
    void shouldCountLikesForComment_ReturnsZeroWhenNoLikes() {
        // Given
        Integer commentId = 100;
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);

        // When
        Long result = commentLikeRepository.countByCommentId(commentId);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(commentLikeRepository).countByCommentId(commentId);
    }

    @Test
    @DisplayName("Should find like record by comment and user - returns record when exists")
    void shouldFindLikeRecordByCommentAndUser_ReturnsRecordWhenExists() {
        // Given
        Integer commentId = 100;
        Integer userId = 1;
        when(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
                .thenReturn(Optional.of(testCommentLike));

        // When
        Optional<CommentLike> result = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testCommentLike);
        verify(commentLikeRepository).findByCommentIdAndUserId(commentId, userId);
    }

    @Test
    @DisplayName("Should find like record by comment and user - returns empty when not exists")
    void shouldFindLikeRecordByCommentAndUser_ReturnsEmptyWhenNotExists() {
        // Given
        Integer commentId = 100;
        Integer userId = 1;
        when(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
                .thenReturn(Optional.empty());

        // When
        Optional<CommentLike> result = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        // Then
        assertThat(result).isEmpty();
        verify(commentLikeRepository).findByCommentIdAndUserId(commentId, userId);
    }

    @Test
    @DisplayName("Should delete like record by comment and user")
    void shouldDeleteLikeRecordByCommentAndUser() {
        // Given
        Integer commentId = 100;
        Integer userId = 1;

        // When
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

        // Then
        verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, userId);
    }

    @Test
    @DisplayName("Should handle null comment ID gracefully")
    void shouldHandleNullCommentIdGracefully() {
        // Given
        Integer userId = 1;

        // When
        boolean existsResult = commentLikeRepository.existsByCommentIdAndUserId(null, userId);
        Long countResult = commentLikeRepository.countByCommentId(null);

        // Then
        verify(commentLikeRepository).existsByCommentIdAndUserId(null, userId);
        verify(commentLikeRepository).countByCommentId(null);
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void shouldHandleNullUserIdGracefully() {
        // Given
        Integer commentId = 100;

        // When
        boolean existsResult = commentLikeRepository.existsByCommentIdAndUserId(commentId, null);
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, null);

        // Then
        verify(commentLikeRepository).existsByCommentIdAndUserId(commentId, null);
        verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, null);
    }
}