package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.CreatePostRequest;
import edu.xtu.bbs.post.exception.SensitiveContentException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.service.PostService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private AuthenticationService authenticationService;

    private PostController controller;

    @BeforeEach
    void setUp() {
        controller = new PostController(postService, authenticationService);
    }

    @Test
    void createPost_shouldPublishWhenAuthenticated() throws SensitiveContentException {
        CreatePostRequest request = new CreatePostRequest("Direct publish title", "Direct publish content", "general");

        User user = new User();
        user.setId(101);
        when(authenticationService.getCurrentUser()).thenReturn(user);

        Post publishedPost = new Post();
        publishedPost.setId(12);
        when(postService.createPost(101, request)).thenReturn(publishedPost);

        Post result = controller.createPost(request);

        assertSame(publishedPost, result);
        verify(postService).createPost(101, request);
    }

    @Test
    void createPost_shouldThrowWhenUnauthenticated() {
        CreatePostRequest request = new CreatePostRequest("No auth", "Content", null);
        when(authenticationService.getCurrentUser()).thenReturn(null);

        SecurityException exception = assertThrows(SecurityException.class, () -> controller.createPost(request));

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(postService);
    }
}
