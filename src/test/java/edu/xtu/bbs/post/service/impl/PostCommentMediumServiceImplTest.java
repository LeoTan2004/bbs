package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.CommentMediaConfiguration;
import edu.xtu.bbs.post.config.PostMediaLimitConfiguration;
import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.exception.TooManyMediaException;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentMediaOssService;
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
class PostCommentMediumServiceImplTest {

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private CommentMediaConfiguration commentMediaConfiguration;

    @Mock
    private PostMediaLimitConfiguration postMediaLimitConfiguration;

    @Mock
    private CommentMediaOssService commentMediaOssService;

    private PostCommentMediumServiceImpl commentMediumService;

    @BeforeEach
    void setUp() {
        commentMediumService = new PostCommentMediumServiceImpl(commentRepository,
                commentMediaConfiguration, postMediaLimitConfiguration, commentMediaOssService);
    }

    @Test
    void uploadMediumToComment_shouldRespectConfiguredCommentLimitWhenLowerThanOssLimit() {
        Integer commentId = 55;
        Integer userId = 7;

        PostComment comment = new PostComment();
        comment.setId(commentId);
        comment.setMedia(new ArrayList<>(List.of(medium("55_1"), medium("55_2"), medium("55_3"))));

        User author = new User();
        author.setId(userId);
        comment.setUser(author);

        Post post = new Post();
        post.setId(3);
        post.setStatus(PostStatus.PUBLISHED);
        comment.setPost(post);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentMediaConfiguration.isTypeAllowed("image/png")).thenReturn(true);
        when(commentMediaConfiguration.getMaxSizeBytes()).thenReturn(1_000_000L);
        when(commentMediaConfiguration.getMaxFiles()).thenReturn(6);
        when(postMediaLimitConfiguration.getMaxCommentMedia()).thenReturn(3);

        CommentMediumUploadRequest request = new CommentMediumUploadRequest("image/png", 512L);

        TooManyMediaException exception = assertThrows(TooManyMediaException.class,
                () -> commentMediumService.uploadMediumToComment(userId, commentId, request));

        assertEquals(3, exception.getMaxAllowedCount());
        verify(commentRepository, never()).save(any(PostComment.class));
        verifyNoInteractions(commentMediaOssService);
    }

    private Medium medium(String id) {
        Medium medium = new Medium();
        medium.setId(id);
        return medium;
    }
}
