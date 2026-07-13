package com.cliuno.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @JsonIgnore
    @OneToMany(mappedBy = "role")
    public List<User> users;
}
