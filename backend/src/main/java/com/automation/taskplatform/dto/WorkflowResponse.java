package com.automation.taskplatform.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private Long id;
    private String name;
    private String triggerType;
    private String cronExpression;
    private String actionType;
    private String actionConfig;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastRunAt;
}
