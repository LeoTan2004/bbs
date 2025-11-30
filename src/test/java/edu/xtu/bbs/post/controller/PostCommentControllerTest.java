package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.CreateCommentRequest;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.service.PostCommentService;
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
class PostCommentControllerTest {

    @Mock
    private PostCommentService postCommentService;

    @Mock
    private AuthenticationService authenticationService;

    private PostCommentController controller;

    @BeforeEach
    void setUp() {
        controller = new PostCommentController(postCommentService, authenticationService);
    }

    @Test
    void createCommentDirect_shouldPublishWhenAuthenticated() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(15, "Direct comment", null);

        User user = new User();
        user.setId(88);
        when(authenticationService.getCurrentUser()).thenReturn(user);

        PostComment publishedComment = new PostComment();
        publishedComment.setId(33);
        when(postCommentService.createComment(88, request)).thenReturn(publishedComment);

        PostComment result = controller.createCommentDirect(request);

        assertSame(publishedComment, result);
        verify(postCommentService).createComment(88, request);
    }

    @Test
    void createCommentDirect_shouldThrowWhenUnauthenticated() {
        CreateCommentRequest request = new CreateCommentRequest(20, "Needs auth", null);
        when(authenticationService.getCurrentUser()).thenReturn(null);

        SecurityException exception = assertThrows(SecurityException.class, () -> controller.createCommentDirect(request));

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(postCommentService);
    }
}
