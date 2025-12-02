package edu.xtu.bbs.notification.model;

import edu.xtu.bbs.notification.converter.NotificationContextConverter;
import edu.xtu.bbs.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "target_user_id, created_at"),
                @Index(name = "idx_notification_user_read", columnList = "target_user_id, is_read, created_at")
        })
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor; // user who triggered the notification

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "redirect_url", length = 1024)
    private String redirectUrl;

    @Column(name = "source_type", length = 100)
    private String sourceType;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "source_snippet", length = 512)
    private String sourceSnippet;

    @Column(name = "context", columnDefinition = "JSON")
    @Convert(converter = NotificationContextConverter.class)
    private Map<String, Object> context = Map.of();

    @Column(name = "is_read", nullable = false)
    private Boolean read = Boolean.FALSE;

    @Column(name = "delivered", nullable = false)
    private Boolean delivered = Boolean.TRUE;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
