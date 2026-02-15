package com.automation.taskplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    @NotBlank(message = "Trigger type is required")
    @Pattern(regexp = "SCHEDULE|MANUAL", message = "Trigger type must be SCHEDULE or MANUAL")
    private String triggerType;

    private String cronExpression;

    @NotBlank(message = "Action type is required")
    private String actionType;

    private String actionConfig;

    private Boolean active = true;
}
