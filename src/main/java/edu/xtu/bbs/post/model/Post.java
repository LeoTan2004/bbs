package edu.xtu.bbs.post.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.xtu.bbs.post.converter.MediumListConverter;
import edu.xtu.bbs.user.model.User;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "media")
    @Convert(converter = MediumListConverter.class)
    private List<Medium> media;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PostStatus status;


    @ColumnDefault("0")
    @Column(name = "possibly_sensitive")
    private Boolean possiblySensitive = false;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToOne(mappedBy = "post")
    @JsonIgnore
    private PublicMatric publicMatrics;

}