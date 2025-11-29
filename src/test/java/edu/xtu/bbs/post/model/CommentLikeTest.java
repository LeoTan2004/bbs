package edu.xtu.bbs.post.model;

import edu.xtu.bbs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommentLike Entity Tests")
class CommentLikeTest {

    private CommentLike commentLike;
    private PostComment comment;
    private User user;

    @BeforeEach
    void setUp() {
        commentLike = new CommentLike();
        comment = new PostComment();
        comment.setId(1);
        
        user = new User();
        user.setId(100);
        user.setUsername("testuser");
    }

    @Test
    @DisplayName("Should create CommentLike with basic properties")
    void shouldCreateCommentLikeWithBasicProperties() {
        // Given
        commentLike.setId(1);
        commentLike.setComment(comment);
        commentLike.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        commentLike.setCreatedAt(now);

        // When & Then
        assertThat(commentLike.getId()).isEqualTo(1);
        assertThat(commentLike.getComment()).isEqualTo(comment);
        assertThat(commentLike.getUser()).isEqualTo(user);
        assertThat(commentLike.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set createdAt automatically on persist")
    void shouldSetCreatedAtAutomaticallyOnPersist() {
        // Given
        LocalDateTime beforePersist = LocalDateTime.now();
        commentLike.setComment(comment);
        commentLike.setUser(user);

        // When
        commentLike.prePersist();
        LocalDateTime afterPersist = LocalDateTime.now();

        // Then
        assertThat(commentLike.getCreatedAt()).isNotNull();
        assertThat(commentLike.getCreatedAt()).isBetween(beforePersist, afterPersist);
    }

    @Test
    @DisplayName("Should not override existing createdAt on persist")
    void shouldNotOverrideExistingCreatedAtOnPersist() {
        // Given
        LocalDateTime existingTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        commentLike.setCreatedAt(existingTime);

        // When
        commentLike.prePersist();

        // Then
        assertThat(commentLike.getCreatedAt()).isEqualTo(existingTime);
    }

    @Test
    @DisplayName("Should handle null comment and user")
    void shouldHandleNullCommentAndUser() {
        // Given & When & Then
        assertThat(commentLike.getComment()).isNull();
        assertThat(commentLike.getUser()).isNull();
        assertThat(commentLike.getCreatedAt()).isNull();
    }
}