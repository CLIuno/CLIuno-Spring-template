package com.cliuno.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String firstName;

    @Column(nullable = false)
    public String lastName;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(unique = true)
    public String phone;

    @JsonIgnore
    @Column(nullable = false)
    public String password;

    public boolean isOnline = false;
    public boolean isVerified = false;
    public boolean isOtpEnabled = false;

    @JsonIgnore
    public String otpBase32;

    @JsonIgnore
    public String otpAuthUrl;

    @JsonIgnore
    public String refreshToken;

    @JsonIgnore
    @Column(name = "reset_token")
    public String resetToken;

    @JsonIgnore
    @Column(name = "verify_token")
    public String verifyToken;

    public boolean isDeleted = false;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "role_id")
    public Role role;
}
