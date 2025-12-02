package edu.xtu.bbs.notification.service;

import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.dto.NotificationResponse;
import edu.xtu.bbs.notification.model.Notification;
import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;
import edu.xtu.bbs.notification.repo.NotificationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User targetUser;
    private User actorUser;

    @BeforeEach
    void setUp() {
        targetUser = new User();
        targetUser.setId(10);
        targetUser.setUsername("target");

        actorUser = new User();
        actorUser.setId(20);
        actorUser.setUsername("actor");
    }

    @Test
    @DisplayName("create should persist notification and return response")
    void createShouldPersistNotification() {
        // Given
        Map<String, Object> context = Map.of("postId", 1);
        NotificationCreateRequest request = new NotificationCreateRequest(
                targetUser.getId(),
                actorUser.getId(),
                NotificationType.POST_COMMENTED,
                NotificationPriority.HIGH,
                "title",
                "content",
                "/posts/1",
                "POST",
                "1",
                "snippet",
                context
        );

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(actorUser.getId())).thenReturn(Optional.of(actorUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(100L);
            notification.setCreatedAt(Instant.now());
            notification.setUpdatedAt(Instant.now());
            return notification;
        });

        // When
        NotificationResponse response = notificationService.create(request);

        // Then
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.type()).isEqualTo(NotificationType.POST_COMMENTED);
        assertThat(response.priority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(response.targetUserId()).isEqualTo(targetUser.getId());
        assertThat(response.actor()).isNotNull();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getTargetUser()).isEqualTo(targetUser);
        assertThat(saved.getActor()).isEqualTo(actorUser);
        assertThat(saved.getContext()).containsEntry("postId", 1);
    }

    @Test
    @DisplayName("create should throw when target user missing")
    void createShouldThrowWhenTargetUserMissing() {
        // Given
        NotificationCreateRequest request = new NotificationCreateRequest(
                999,
                null,
                NotificationType.POST_LIKED,
                null,
                "title",
                "content",
                null,
                null,
                null,
                null,
                Map.of()
        );

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> notificationService.create(request));
    }

    @Test
    @DisplayName("markAsRead should update read status")
    void markAsReadShouldUpdateReadStatus() {
        // Given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setTargetUser(targetUser);
        notification.setRead(Boolean.FALSE);

        when(notificationRepository.findByIdAndTargetUserId(1L, targetUser.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        // When
        NotificationResponse response = notificationService.markAsRead(targetUser.getId(), 1L);

        // Then
        assertThat(response.read()).isTrue();
        assertThat(notification.getRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAllAsRead should update all unread notifications")
    void markAllAsReadShouldUpdateAllUnreadNotifications() {
        // Given
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setTargetUser(targetUser);
        n1.setRead(Boolean.FALSE);

        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setTargetUser(targetUser);
        n2.setRead(Boolean.FALSE);

        when(notificationRepository.findByTargetUserIdAndReadFalse(targetUser.getId()))
                .thenReturn(List.of(n1, n2));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int updated = notificationService.markAllAsRead(targetUser.getId());

        // Then
        assertThat(updated).isEqualTo(2);
        assertThat(n1.getRead()).isTrue();
        assertThat(n2.getRead()).isTrue();
        verify(notificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("countUnread should delegate to repository")
    void countUnreadShouldDelegateToRepository() {
        when(notificationRepository.countByTargetUserIdAndReadFalse(targetUser.getId())).thenReturn(5L);

        long unread = notificationService.countUnread(targetUser.getId());

        assertThat(unread).isEqualTo(5L);
        verify(notificationRepository).countByTargetUserIdAndReadFalse(targetUser.getId());
    }

    @Test
    @DisplayName("getNotifications should return mapped page")
    void getNotificationsShouldReturnMappedPage() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setTargetUser(targetUser);
        notification.setActor(actorUser);
        notification.setType(NotificationType.POST_LIKED);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setRead(Boolean.FALSE);
        notification.setContext(Map.of());
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));

        when(notificationRepository.findByTargetUserId(eq(targetUser.getId()), any(Pageable.class)))
                .thenReturn(notificationPage);

        Page<NotificationResponse> responsePage = notificationService.getNotifications(targetUser.getId(), PageRequest.of(0, 10));

        assertThat(responsePage.getContent()).hasSize(1);
        assertThat(responsePage.getContent().get(0).type()).isEqualTo(NotificationType.POST_LIKED);
    }
}
