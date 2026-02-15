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

@Entity //create a entity for execution logs
@Table(name = "execution_logs")
@Data
@NoArgsConstructor 
@AllArgsConstructor
public class ExecutionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Many execution logs can belong to one workflow 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    // column lets postgres know that this is a foreign key
    @Column(nullable = false) // Status of execution
    private String status; // "SUCCESS" or "FAILURE"
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt = LocalDateTime.now();
    
    private Integer executionTimeMs; // How long it took in milliseconds
    
    @PrePersist
    protected void onCreate() {
        executedAt = LocalDateTime.now();
    }



}