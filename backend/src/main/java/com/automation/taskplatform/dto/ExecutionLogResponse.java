package com.automation.taskplatform.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogResponse {

    private Long id;
    private Long workflowId;
    private String workflowName;
    private String status;
    private String errorMessage;
    private LocalDateTime executedAt;
    private Integer executionTimeMs;
}
