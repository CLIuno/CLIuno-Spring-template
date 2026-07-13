package com.cliuno.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String title;

    public String description = "";

    public boolean isCompleted = false;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;
}
