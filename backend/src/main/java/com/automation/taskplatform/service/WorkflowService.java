package com.automation.taskplatform.service;
import org.springframework.stereotype.Service;
import com.automation.taskplatform.repository.WorkflowRepository;
import com.automation.taskplatform.model.Workflow;
import com.automation.taskplatform.model.User;
import org.springframework.security.core.context.SecurityContextHolder;
import com.automation.taskplatform.repository.UserRepository;
import java.util.List;

@Service
public class WorkflowService {

      private final WorkflowRepository workflowRepository;
      private final UserRepository userRepository;

      public WorkflowService(WorkflowRepository workflowRepository, UserRepository userRepository) {
          this.workflowRepository = workflowRepository;
          this.userRepository = userRepository;
      }

      public Workflow createWorkflow(Workflow workflow) {                                                                                               
          // Get current user's email from JWT token                                                                                                    
          String email = SecurityContextHolder.getContext().getAuthentication().getName();                                                              
                                                                                                                                                        
          // Find user in database                                                                                                                      
          User user = userRepository.findByEmailIgnoreCase(email)                                                                                                 
              .orElseThrow(() -> new RuntimeException("User not found"));                                                                               
                                                                                                                                                        
          // Link workflow to user                                                                                                                      
          workflow.setUser(user);                                                                                                                       
                                                                                                                                                        
          // Save and return                                                                                                                            
          return workflowRepository.save(workflow);                                                                                                     
      } 

      public List<Workflow> getWorkflowsByUser() {                                                                                                          
      String email = SecurityContextHolder.getContext().getAuthentication().getName();                                                                  
      User user = userRepository.findByEmailIgnoreCase(email)                                                                                                     
          .orElseThrow(() -> new RuntimeException("User not found"));                                                                                   
      return workflowRepository.findByUser(user);                                                                                                       
  }                                                                                                                                                     
                                                                                                                                                        
    public Workflow getWorkflowById(Long id) {                                                                                                            
      return workflowRepository.findById(id)                                                                                                            
          .orElseThrow(() -> new RuntimeException("Workflow not found"));                                                                               
  }                                                                                                                                                     
                                                                                                                                                        
    public Workflow updateWorkflow(Long id, Workflow updated) {
      Workflow existing = getWorkflowById(id);
      existing.setName(updated.getName());
      existing.setTriggerType(updated.getTriggerType());
      existing.setActionType(updated.getActionType());
      existing.setCronExpression(updated.getCronExpression());
      existing.setActionConfig(updated.getActionConfig());
      existing.setActive(updated.isActive());
      return workflowRepository.save(existing);
  }                                                                                                                                                     
                                                                                                                                                        
    public void deleteWorkflow(Long id) {                                                                                                                 
      workflowRepository.deleteById(id);                                                                                                                
  }                               
                                                                                                                            
  } 