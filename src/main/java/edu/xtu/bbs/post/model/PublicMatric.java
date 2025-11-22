package edu.xtu.bbs.post.model;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "public_matric")
public class PublicMatric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ColumnDefault("0")
    @Column(name = "views")
    private Integer views;

    @ColumnDefault("0")
    @Column(name = "likes")
    private Integer likes;

    @ColumnDefault("0")
    @Column(name = "favorites")
    private Integer favorites;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

}