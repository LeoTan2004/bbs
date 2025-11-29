package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.CommentResponse;
import edu.xtu.bbs.post.dto.CreateCommentRequest;
import edu.xtu.bbs.post.dto.UpdateCommentRequest;
import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.exception.CommentPermissionDeniedException;
import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.model.CommentStatus;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.impl.PostCommentServiceImpl;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicMatricRepository publicMatricRepository;

    private PostCommentService commentService;

    private User testUser;
    private Post testPost;
    private PostComment testComment;

    @BeforeEach
    void setUp() {
        commentService = new PostCommentServiceImpl(commentRepository, postRepository, userRepository, publicMatricRepository);

        // Setup test data
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setAvatarUrl("avatar.jpg");

        testPost = new Post();
        testPost.setId(1);
        testPost.setStatus(PostStatus.PUBLISHED);

        testComment = new PostComment();
        testComment.setId(1);
        testComment.setPost(testPost);
        testComment.setUser(testUser);
        testComment.setContent("Test comment");
        testComment.setComments(0);
        testComment.setLikes(0);
        testComment.setCreatedAt(Instant.now());
        testComment.setUpdatedAt(Instant.now());
        testComment.setMedia(Collections.emptyList());
        testComment.setStatus(CommentStatus.PUBLISHED);
    }

    @Test
    void testCreateComment_Success() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(1, "Test comment", null);

        when(postRepository.findByIdAndStatus(1, PostStatus.PUBLISHED))
                .thenReturn(Optional.of(testPost));
        when(userRepository.findById(1))
                .thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(PostComment.class)))
                .thenReturn(testComment);

        // When
        PostComment result = commentService.createComment(1, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test comment", result.getContent());
        verify(commentRepository).save(any(PostComment.class));
    }

    @Test
    void testCreateComment_PostNotFound() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(999, "Test comment", null);

        when(postRepository.findByIdAndStatus(999, PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());
        when(postRepository.findById(999))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(PostNotFoundException.class, () -> 
                commentService.createComment(1, request));
    }

    @Test
    void testUpdateComment_Success() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest("Updated comment");

        when(commentRepository.findById(1))
                .thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(PostComment.class)))
                .thenReturn(testComment);

        // When
        PostComment result = commentService.updateComment(1, 1, request);

        // Then
        assertNotNull(result);
        verify(commentRepository).save(any(PostComment.class));
    }

    @Test
    void testUpdateComment_PermissionDenied() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest("Updated comment");

        when(commentRepository.findById(1))
                .thenReturn(Optional.of(testComment));

        // When & Then - Different user trying to update
        assertThrows(CommentPermissionDeniedException.class, () -> 
                commentService.updateComment(2, 1, request));
    }

    @Test
    void testDeleteComment_Success() {
        // Given
        when(commentRepository.findById(1))
                .thenReturn(Optional.of(testComment));

        // When
        assertDoesNotThrow(() -> commentService.deleteComment(1, 1));

        // Then
        verify(commentRepository).delete(testComment);
    }

    @Test
    void testGetCommentResponseById_Success() {
        // Given
        when(commentRepository.findById(1))
                .thenReturn(Optional.of(testComment));

        // When
        CommentResponse result = commentService.getCommentResponseById(1, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("Test comment", result.content());
    }

    @Test
    void testGetCommentsByPost_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<PostComment> mockPage = new PageImpl<>(Collections.singletonList(testComment));

        when(postRepository.existsById(1)).thenReturn(true);
        when(commentRepository.findByPostIdAndParentPostIdIsNullAndStatus(eq(1), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<PostComment> result = commentService.getCommentsByPost(1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetCommentResponsesByPost_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<PostComment> mockPage = new PageImpl<>(Collections.singletonList(testComment));

        when(postRepository.existsById(1)).thenReturn(true);
        when(commentRepository.findByPostIdAndParentPostIdIsNullAndStatus(eq(1), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        Page<CommentResponse> result = commentService.getCommentResponsesByPost(1, 1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test comment", result.getContent().get(0).content());
    }

    @Test
    void testCountCommentsByPost() {
        // Given
        when(commentRepository.countByPostIdAndStatus(1, CommentStatus.PUBLISHED)).thenReturn(5L);

        // When
        Integer result = commentService.countCommentsByPost(1);

        // Then
        assertEquals(5, result);
    }

    @Test
    void testCountTopLevelCommentsByPost() {
        // Given
        when(commentRepository.countByPostIdAndParentPostIdIsNullAndStatus(1, CommentStatus.PUBLISHED)).thenReturn(3L);

        // When
        Integer result = commentService.countTopLevelCommentsByPost(1);

        // Then
        assertEquals(3, result);
    }

    @Test
    void testCountCommentsByUser() {
        // Given
        when(commentRepository.countByUserIdAndStatus(1, CommentStatus.PUBLISHED)).thenReturn(7L);

        // When
        Integer result = commentService.countCommentsByUser(1);

        // Then
        assertEquals(7, result);
    }

    @Test
    void testHasModifyPermission_Success() {
        // Given
        when(commentRepository.findById(1)).thenReturn(Optional.of(testComment));

        // When
        Boolean result = commentService.hasModifyPermission(1, 1);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasModifyPermission_PermissionDenied() {
        // Given
        when(commentRepository.findById(1)).thenReturn(Optional.of(testComment));

        // When
        Boolean result = commentService.hasModifyPermission(2, 1);

        // Then
        assertFalse(result);
    }

    @Test
    void testHasModifyPermission_CommentNotFound() {
        // Given
        when(commentRepository.findById(999)).thenReturn(Optional.empty());

        // When
        Boolean result = commentService.hasModifyPermission(1, 999);

        // Then
        assertFalse(result);
    }
}