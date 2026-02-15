package com.automation.taskplatform.repository;

import com.automation.taskplatform.model.User;
import com.automation.taskplatform.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    
    List<Workflow> findByUser(User user);
    
    List<Workflow> findByUserAndActiveTrue(User user);
    
    List<Workflow> findByActiveTrueAndTriggerType(String triggerType);
}