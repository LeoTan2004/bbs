package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.model.CommentLike;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.repo.CommentLikeRepository;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentLikeService Tests")
class CommentLikeServiceImplTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentLikeServiceImpl commentLikeService;

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

    @Nested
    @DisplayName("hasLikedComment Tests")
    class HasLikedCommentTests {

        @Test
        @DisplayName("Should return true when user has liked the comment")
        void shouldReturnTrueWhenUserHasLikedComment() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);

            // When
            Boolean result = commentLikeService.hasLikedComment(userId, commentId);

            // Then
            assertThat(result).isTrue();
            verify(commentLikeRepository).existsByCommentIdAndUserId(commentId, userId);
        }

        @Test
        @DisplayName("Should return false when user has not liked the comment")
        void shouldReturnFalseWhenUserHasNotLikedComment() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);

            // When
            Boolean result = commentLikeService.hasLikedComment(userId, commentId);

            // Then
            assertThat(result).isFalse();
            verify(commentLikeRepository).existsByCommentIdAndUserId(commentId, userId);
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
            // Given
            Integer commentId = 100;

            // When
            Boolean result = commentLikeService.hasLikedComment(null, commentId);

            // Then
            assertThat(result).isFalse();
            // Should not call repository when userId is null
            verify(commentLikeRepository, never()).existsByCommentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should handle null comment ID gracefully")
        void shouldHandleNullCommentIdGracefully() {
            // Given
            Integer userId = 1;

            // When
            Boolean result = commentLikeService.hasLikedComment(userId, null);

            // Then
            assertThat(result).isFalse();
            // Should not call repository when commentId is null
            verify(commentLikeRepository, never()).existsByCommentIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("likeComment Tests")
    class LikeCommentTests {

        @Test
        @DisplayName("Should successfully like a comment when user and comment exist")
        void shouldSuccessfullyLikeCommentWhenUserAndCommentExist() throws UserNotFoundException, CommentNotFoundException {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);
            when(commentLikeRepository.save(any(CommentLike.class))).thenReturn(testCommentLike);
            testComment.setLikes(5);
            when(commentRepository.save(testComment)).thenReturn(testComment);

            // When
            commentLikeService.likeComment(userId, commentId);

            // Then
            ArgumentCaptor<CommentLike> captor = ArgumentCaptor.forClass(CommentLike.class);
            verify(commentLikeRepository).save(captor.capture());
            CommentLike savedLike = captor.getValue();
            assertThat(savedLike.getUser()).isEqualTo(testUser);
            assertThat(savedLike.getComment()).isEqualTo(testComment);
            verify(commentRepository).save(testComment);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
            // Given
            Integer userId = 999;
            Integer commentId = 100;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository).findById(userId);
            verify(commentRepository, never()).findById(any());
            verify(commentLikeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() {
            // Given
            Integer userId = 1;
            Integer commentId = 999;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(userRepository).findById(userId);
            verify(commentRepository).findById(commentId);
            verify(commentLikeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when user already liked the comment")
        void shouldThrowIllegalStateExceptionWhenUserAlreadyLikedComment() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("User has already liked this comment");
            verify(commentLikeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unlikeComment Tests")
    class UnlikeCommentTests {

        @Test
        @DisplayName("Should successfully unlike a comment when user has liked it")
        void shouldSuccessfullyUnlikeCommentWhenUserHasLikedIt() throws UserNotFoundException, CommentNotFoundException {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);
            testComment.setLikes(5);
            when(commentRepository.save(testComment)).thenReturn(testComment);

            // When
            commentLikeService.unlikeComment(userId, commentId);

            // Then
            verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, userId);
            verify(commentRepository).save(testComment);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
            // Given
            Integer userId = 999;
            Integer commentId = 100;
            when(userRepository.existsById(userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(userId, commentId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository).existsById(userId);
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() {
            // Given
            Integer userId = 1;
            Integer commentId = 999;
            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(userId, commentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(userRepository).existsById(userId);
            verify(commentRepository).findById(commentId);
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when user has not liked the comment")
        void shouldThrowIllegalStateExceptionWhenUserHasNotLikedComment() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(userId, commentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("User has not liked this comment");
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("getCommentLikeCount Tests")
    class GetCommentLikeCountTests {

        @Test
        @DisplayName("Should return correct like count for comment")
        void shouldReturnCorrectLikeCountForComment() {
            // Given
            Integer commentId = 100;
            Long expectedCount = 5L;
            when(commentLikeRepository.countByCommentId(commentId)).thenReturn(expectedCount);

            // When
            Long result = commentLikeService.getCommentLikeCount(commentId);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(commentLikeRepository).countByCommentId(commentId);
        }

        @Test
        @DisplayName("Should return zero when comment has no likes")
        void shouldReturnZeroWhenCommentHasNoLikes() {
            // Given
            Integer commentId = 100;
            when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);

            // When
            Long result = commentLikeService.getCommentLikeCount(commentId);

            // Then
            assertThat(result).isEqualTo(0L);
            verify(commentLikeRepository).countByCommentId(commentId);
        }

        @Test
        @DisplayName("Should handle null comment ID gracefully")
        void shouldHandleNullCommentIdGracefully() {
            // When
            Long result = commentLikeService.getCommentLikeCount(null);

            // Then
            assertThat(result).isEqualTo(0L);
            // Should not call repository when commentId is null
            verify(commentLikeRepository, never()).countByCommentId(any());
        }
    }

    @Nested
    @DisplayName("toggleCommentLike Tests")
    class ToggleCommentLikeTests {

        @Test
        @DisplayName("Should like comment when user has not liked it before")
        void shouldLikeCommentWhenUserHasNotLikedItBefore() throws UserNotFoundException, CommentNotFoundException {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.save(any(CommentLike.class))).thenReturn(testCommentLike);
            testComment.setLikes(5);
            when(commentRepository.save(testComment)).thenReturn(testComment);

            // When
            Boolean result = commentLikeService.toggleCommentLike(userId, commentId);

            // Then
            assertThat(result).isTrue();
            verify(commentLikeRepository).save(any(CommentLike.class));
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should unlike comment when user has already liked it")
        void shouldUnlikeCommentWhenUserHasAlreadyLikedIt() throws UserNotFoundException, CommentNotFoundException {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);
            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            testComment.setLikes(5);
            when(commentRepository.save(testComment)).thenReturn(testComment);

            // When
            Boolean result = commentLikeService.toggleCommentLike(userId, commentId);

            // Then
            assertThat(result).isFalse();
            verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, userId);
            verify(commentLikeRepository, never()).save(any(CommentLike.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
            // Given
            Integer userId = 999;
            Integer commentId = 100;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.toggleCommentLike(userId, commentId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository).findById(userId);
            verify(commentLikeRepository, never()).save(any());
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() {
            // Given
            Integer userId = 1;
            Integer commentId = 999;
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.toggleCommentLike(userId, commentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(userRepository).findById(userId);
            verify(commentRepository).findById(commentId);
            verify(commentLikeRepository, never()).save(any());
            verify(commentLikeRepository, never()).deleteByCommentIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle database exceptions gracefully in like operation")
        void shouldHandleDatabaseExceptionsGracefullyInLikeOperation() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(false);
            when(commentLikeRepository.save(any(CommentLike.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle database exceptions gracefully in unlike operation")
        void shouldHandleDatabaseExceptionsGracefullyInUnlikeOperation() {
            // Given
            Integer userId = 1;
            Integer commentId = 100;
            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).thenReturn(true);
            doThrow(new RuntimeException("Database error")).when(commentLikeRepository)
                    .deleteByCommentIdAndUserId(commentId, userId);

            // When & Then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(userId, commentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle negative user ID gracefully")
        void shouldHandleNegativeUserId() {
            // Given
            Integer userId = -1;
            Integer commentId = 100;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should handle negative comment ID gracefully")
        void shouldHandleNegativeCommentId() {
            // Given
            Integer userId = 1;
            Integer commentId = -100;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentLikeService.likeComment(userId, commentId))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}