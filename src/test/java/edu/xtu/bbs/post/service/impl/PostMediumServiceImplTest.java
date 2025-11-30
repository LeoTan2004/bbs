package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.MediaConfiguration;
import edu.xtu.bbs.post.config.PostMediaLimitConfiguration;
import edu.xtu.bbs.post.dto.PostMediumUploadRequest;
import edu.xtu.bbs.post.exception.TooManyMediaException;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.service.MediaOssService;
import edu.xtu.bbs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostMediumServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MediaConfiguration mediaConfiguration;

    @Mock
    private PostMediaLimitConfiguration postMediaLimitConfiguration;

    @Mock
    private MediaOssService mediaOssService;

    private PostMediumServiceImpl postMediumService;

    @BeforeEach
    void setUp() {
        postMediumService = new PostMediumServiceImpl(postRepository, mediaConfiguration,
                postMediaLimitConfiguration, mediaOssService);
    }

    @Test
    void uploadMediumToDraft_shouldRespectConfiguredPostLimitWhenLowerThanOssLimit() {
        Integer postId = 10;
        Integer userId = 42;

        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.DRAFT);
        post.setMedia(new ArrayList<>(List.of(medium("10_1"), medium("10_2"))));

        User author = new User();
        author.setId(userId);
        post.setAuthor(author);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(mediaConfiguration.isTypeAllowed("image/png")).thenReturn(true);
        when(mediaConfiguration.getMaxSizeBytes()).thenReturn(1_000_000L);
        when(mediaConfiguration.getMaxFiles()).thenReturn(5);
        when(postMediaLimitConfiguration.getMaxPostMedia()).thenReturn(2);

        PostMediumUploadRequest request = new PostMediumUploadRequest("image/png", 512L);

        TooManyMediaException exception = assertThrows(TooManyMediaException.class,
                () -> postMediumService.uploadMediumToDraft(userId, postId, request));

        assertEquals(2, exception.getMaxAllowedCount());
        verify(postRepository, never()).save(any(Post.class));
        verifyNoInteractions(mediaOssService);
    }

    private Medium medium(String id) {
        Medium medium = new Medium();
        medium.setId(id);
        return medium;
    }
}
