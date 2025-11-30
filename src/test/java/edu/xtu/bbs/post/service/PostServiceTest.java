package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.CreatePostRequest;
import edu.xtu.bbs.post.exception.SensitiveContentException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PublicMatricRepository publicMatricRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl(postRepository, publicMatricRepository);
    }

    @Test
    void testCreatePost_Success() throws SensitiveContentException {
        CreatePostRequest request = new CreatePostRequest("Hello World", "This is a post body", "general");

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1);
            return post;
        });
        when(publicMatricRepository.save(any(PublicMatric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.createPost(10, request);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(PostStatus.PUBLISHED, result.getStatus());
        assertEquals("Hello World", result.getTitle());

        verify(postRepository).save(any(Post.class));
        verify(publicMatricRepository).save(any(PublicMatric.class));
    }

    @Test
    void testCreatePost_SensitiveContent() {
        CreatePostRequest request = new CreatePostRequest("spam warning", "This post mentions spam keyword", null);

        assertThrows(SensitiveContentException.class, () -> postService.createPost(5, request));
        verifyNoInteractions(postRepository, publicMatricRepository);
    }
}
