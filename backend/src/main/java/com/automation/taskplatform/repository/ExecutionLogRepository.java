package com.automation.taskplatform.repository;

import com.automation.taskplatform.model.ExecutionLog;
import com.automation.taskplatform.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    
    List<ExecutionLog> findByWorkflowOrderByExecutedAtDesc(Workflow workflow);
    
    List<ExecutionLog> findTop10ByWorkflowOrderByExecutedAtDesc(Workflow workflow);
}