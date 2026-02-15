package com.automation.taskplatform.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workflows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String triggerType; // "SCHEDULE" or "MANUAL"
    
    @Column(length = 100)
    private String cronExpression; // e.g., "0 0 9 * * ?" for 9 AM daily
    
    @Column(nullable = false)
    private String actionType; // "SEND_EMAIL"
    
    @Column(columnDefinition = "TEXT")
    private String actionConfig; // JSON string with email details
    
    @Column(nullable = false)
    private boolean active = true;
    
    @ManyToOne(fetch = FetchType.LAZY) // Many workflows can belong to one user
    @JoinColumn(name = "user_id", nullable = false) // Foreign key column
    private User user;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastRunAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}