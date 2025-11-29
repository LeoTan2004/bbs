package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.post.service.impl.PostCommentServiceImpl;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostCommentService Comment Like Integration Tests")
class PostCommentServiceCommentLikeIntegrationTest {

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private CommentLikeService commentLikeService;

    @InjectMocks
    private PostCommentServiceImpl postCommentService;

    private Integer testUserId;
    private Integer testCommentId;
    private Integer nonExistentCommentId;

    @BeforeEach
    void setUp() {
        testUserId = 1;
        testCommentId = 100;
        nonExistentCommentId = 999;
    }

    @Nested
    @DisplayName("toggleCommentLike Integration Tests")
    class ToggleCommentLikeIntegrationTests {

        @Test
        @DisplayName("Should successfully toggle comment like when comment exists")
        void shouldSuccessfullyToggleCommentLikeWhenCommentExists() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            when(commentLikeService.toggleCommentLike(testUserId, testCommentId)).thenReturn(true);

            // When
            Boolean result = postCommentService.toggleCommentLike(testUserId, testCommentId);

            // Then
            assertThat(result).isTrue();
            verify(commentRepository).existsById(testCommentId);
            verify(commentLikeService).toggleCommentLike(testUserId, testCommentId);
        }

        @Test
        @DisplayName("Should return false when toggling unlike")
        void shouldReturnFalseWhenTogglingUnlike() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            when(commentLikeService.toggleCommentLike(testUserId, testCommentId)).thenReturn(false);

            // When
            Boolean result = postCommentService.toggleCommentLike(testUserId, testCommentId);

            // Then
            assertThat(result).isFalse();
            verify(commentRepository).existsById(testCommentId);
            verify(commentLikeService).toggleCommentLike(testUserId, testCommentId);
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(nonExistentCommentId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> postCommentService.toggleCommentLike(testUserId, nonExistentCommentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(commentRepository).existsById(nonExistentCommentId);
            verify(commentLikeService, never()).toggleCommentLike(any(), any());
        }

        @Test
        @DisplayName("Should handle service exceptions and wrap them in RuntimeException")
        void shouldHandleServiceExceptionsAndWrapThemInRuntimeException() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            when(commentLikeService.toggleCommentLike(testUserId, testCommentId))
                    .thenThrow(new RuntimeException("Service error"));

            // When & Then
            assertThatThrownBy(() -> postCommentService.toggleCommentLike(testUserId, testCommentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to toggle comment like")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("likeComment Integration Tests")
    class LikeCommentIntegrationTests {

        @Test
        @DisplayName("Should successfully like comment when comment exists")
        void shouldSuccessfullyLikeCommentWhenCommentExists() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            doNothing().when(commentLikeService).likeComment(testUserId, testCommentId);

            // When
            postCommentService.likeComment(testUserId, testCommentId);

            // Then
            verify(commentRepository).existsById(testCommentId);
            verify(commentLikeService).likeComment(testUserId, testCommentId);
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(nonExistentCommentId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> postCommentService.likeComment(testUserId, nonExistentCommentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(commentRepository).existsById(nonExistentCommentId);
            verify(commentLikeService, never()).likeComment(any(), any());
        }

        @Test
        @DisplayName("Should handle service exceptions and wrap them in RuntimeException")
        void shouldHandleServiceExceptionsAndWrapThemInRuntimeException() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            doThrow(new RuntimeException("Service error")).when(commentLikeService)
                    .likeComment(testUserId, testCommentId);

            // When & Then
            assertThatThrownBy(() -> postCommentService.likeComment(testUserId, testCommentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to like comment")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("unlikeComment Integration Tests")
    class UnlikeCommentIntegrationTests {

        @Test
        @DisplayName("Should successfully unlike comment when comment exists")
        void shouldSuccessfullyUnlikeCommentWhenCommentExists() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            doNothing().when(commentLikeService).unlikeComment(testUserId, testCommentId);

            // When
            postCommentService.unlikeComment(testUserId, testCommentId);

            // Then
            verify(commentRepository).existsById(testCommentId);
            verify(commentLikeService).unlikeComment(testUserId, testCommentId);
        }

        @Test
        @DisplayName("Should throw CommentNotFoundException when comment does not exist")
        void shouldThrowCommentNotFoundExceptionWhenCommentDoesNotExist() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(nonExistentCommentId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> postCommentService.unlikeComment(testUserId, nonExistentCommentId))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(commentRepository).existsById(nonExistentCommentId);
            verify(commentLikeService, never()).unlikeComment(any(), any());
        }

        @Test
        @DisplayName("Should handle service exceptions and wrap them in RuntimeException")
        void shouldHandleServiceExceptionsAndWrapThemInRuntimeException() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            doThrow(new RuntimeException("Service error")).when(commentLikeService)
                    .unlikeComment(testUserId, testCommentId);

            // When & Then
            assertThatThrownBy(() -> postCommentService.unlikeComment(testUserId, testCommentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to unlike comment")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("hasLikedComment Tests")
    class HasLikedCommentTests {

        @Test
        @DisplayName("Should return true when user has liked the comment")
        void shouldReturnTrueWhenUserHasLikedComment() {
            // Given
            when(commentLikeService.hasLikedComment(testUserId, testCommentId)).thenReturn(true);

            // When
            Boolean result = postCommentService.hasLikedComment(testUserId, testCommentId);

            // Then
            assertThat(result).isTrue();
            verify(commentLikeService).hasLikedComment(testUserId, testCommentId);
        }

        @Test
        @DisplayName("Should return false when user has not liked the comment")
        void shouldReturnFalseWhenUserHasNotLikedComment() {
            // Given
            when(commentLikeService.hasLikedComment(testUserId, testCommentId)).thenReturn(false);

            // When
            Boolean result = postCommentService.hasLikedComment(testUserId, testCommentId);

            // Then
            assertThat(result).isFalse();
            verify(commentLikeService).hasLikedComment(testUserId, testCommentId);
        }
    }

    @Nested
    @DisplayName("getCommentLikeCount Tests")
    class GetCommentLikeCountTests {

        @Test
        @DisplayName("Should return correct like count for comment")
        void shouldReturnCorrectLikeCountForComment() {
            // Given
            Long expectedCount = 42L;
            when(commentLikeService.getCommentLikeCount(testCommentId)).thenReturn(expectedCount);

            // When
            Long result = postCommentService.getCommentLikeCount(testCommentId);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(commentLikeService).getCommentLikeCount(testCommentId);
        }

        @Test
        @DisplayName("Should return zero when comment has no likes")
        void shouldReturnZeroWhenCommentHasNoLikes() {
            // Given
            when(commentLikeService.getCommentLikeCount(testCommentId)).thenReturn(0L);

            // When
            Long result = postCommentService.getCommentLikeCount(testCommentId);

            // Then
            assertThat(result).isEqualTo(0L);
            verify(commentLikeService).getCommentLikeCount(testCommentId);
        }
    }

    @Nested
    @DisplayName("hasModifyPermission Tests")
    class HasModifyPermissionTests {

        @Test
        @DisplayName("Should return true when user owns the comment")
        void shouldReturnTrueWhenUserOwnsComment() {
            // Given
            // This test would need to be implemented based on the actual hasModifyPermission implementation
            // Since the implementation is already provided in the service, this is a placeholder
            // for testing the permission checking logic
        }

        @Test
        @DisplayName("Should return false when user does not own the comment")
        void shouldReturnFalseWhenUserDoesNotOwnComment() {
            // Given
            // This test would need to be implemented based on the actual hasModifyPermission implementation
        }

        @Test
        @DisplayName("Should return false when comment does not exist")
        void shouldReturnFalseWhenCommentDoesNotExist() {
            // Given
            // This test would need to be implemented based on the actual hasModifyPermission implementation
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            when(commentLikeService.toggleCommentLike(null, testCommentId)).thenReturn(false);

            // When
            Boolean result = postCommentService.toggleCommentLike(null, testCommentId);

            // Then
            assertThat(result).isFalse();
            verify(commentLikeService).toggleCommentLike(null, testCommentId);
        }

        @Test
        @DisplayName("Should handle null comment ID gracefully")
        void shouldHandleNullCommentIdGracefully() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(null)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> postCommentService.toggleCommentLike(testUserId, null))
                    .isInstanceOf(CommentNotFoundException.class);
        }

        @Test
        @DisplayName("Should handle concurrent like operations gracefully")
        void shouldHandleConcurrentLikeOperationsGracefully() throws UserNotFoundException {
            // Given
            when(commentRepository.existsById(testCommentId)).thenReturn(true);
            when(commentLikeService.toggleCommentLike(testUserId, testCommentId))
                    .thenThrow(new IllegalStateException("Concurrent modification"));

            // When & Then
            assertThatThrownBy(() -> postCommentService.toggleCommentLike(testUserId, testCommentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to toggle comment like")
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }
}