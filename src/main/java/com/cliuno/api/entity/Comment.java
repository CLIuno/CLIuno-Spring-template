package com.cliuno.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String content;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    public Post post;
}
