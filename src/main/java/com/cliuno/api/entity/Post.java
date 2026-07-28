package com.cliuno.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String title;

    @Column(nullable = false)
    public String content;

    @JsonProperty("image_url")
    public String imageUrl;

    public boolean isPaid = false;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    public List<Comment> comments = new ArrayList<>();
}
