package com.automation.taskplatform.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor // comes from lombok to generate no-arg constructor
@AllArgsConstructor // comes from lombok to generate all-args constructor
public class User {
    
    @Id // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String firstName;

    private String lastName;

    // Google OAuth tokens
    @Column(columnDefinition = "TEXT")
    private String googleAccessToken;

    @Column(columnDefinition = "TEXT")
    private String googleRefreshToken;

    private LocalDateTime googleTokenExpiry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}