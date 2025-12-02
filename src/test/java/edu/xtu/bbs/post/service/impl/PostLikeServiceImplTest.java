package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.service.NotificationService;
import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostLike;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostLikeRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceImplTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PublicMatricRepository publicMatricRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostLikeServiceImpl postLikeService;

    private Post post;
    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(2);
        author.setUsername("author");

        post = new Post();
        post.setId(100);
        post.setStatus(PostStatus.PUBLISHED);
        post.setAuthor(author);
        post.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("likePost should send notification to author")
    void likePostShouldSendNotification() throws PostNotFoundException, PostStatusNotAllowedException {
        when(postRepository.findByIdAndStatus(post.getId(), PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndUserId(post.getId(), 1)).thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postLikeRepository.countByPostId(post.getId())).thenReturn(1L);
        when(publicMatricRepository.findByPostId(post.getId())).thenReturn(Optional.empty());
        when(publicMatricRepository.save(any(PublicMatric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationService.create(any(NotificationCreateRequest.class))).thenReturn(null);

        Boolean result = postLikeService.likePost(1, post.getId());

        assertThat(result).isTrue();
        ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().targetUserId()).isEqualTo(author.getId());
        assertThat(captor.getValue().actorUserId()).isEqualTo(1);
    }

    @Test
    @DisplayName("likePost should not send notification when author likes own post")
    void likePostShouldNotNotifyWhenAuthorLikesOwnPost() throws PostNotFoundException, PostStatusNotAllowedException {
        when(postRepository.findByIdAndStatus(post.getId(), PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndUserId(post.getId(), author.getId())).thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postLikeRepository.countByPostId(post.getId())).thenReturn(1L);
        when(publicMatricRepository.findByPostId(post.getId())).thenReturn(Optional.empty());
        when(publicMatricRepository.save(any(PublicMatric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Boolean result = postLikeService.likePost(author.getId(), post.getId());

        assertThat(result).isTrue();
        verify(notificationService, never()).create(any());
    }
}
