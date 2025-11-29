package edu.xtu.bbs.post.model;

import edu.xtu.bbs.post.converter.MediumListConverter;
import edu.xtu.bbs.user.model.User;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "post_comment")
public class PostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "parent_post_id")
    private Integer parentPostId;

    @Column(name = "media")
    @Convert(converter = MediumListConverter.class)
    private List<Medium> media;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.PUBLISHED;

    @ColumnDefault("0")
    @Column(name = "comments")
    private Integer comments;

    @ColumnDefault("0")
    @Column(name = "likes")
    private Integer likes;


    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

}