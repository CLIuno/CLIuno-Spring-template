package com.cliuno.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id")
    public User follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "following_id")
    public User following;

    public Instant createdAt = Instant.now();
}
