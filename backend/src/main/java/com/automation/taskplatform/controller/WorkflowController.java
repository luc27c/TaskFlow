package com.automation.taskplatform.controller;                                                                                                       
                                                                                                                                                        
  import java.util.List;                                                                                                                                
                                                                                                                                                        
  import org.springframework.http.ResponseEntity;                                                                                                       
  import org.springframework.web.bind.annotation.*;                                                                                                     
                                                                                                                                                        
  import com.automation.taskplatform.model.Workflow;
  import com.automation.taskplatform.service.WorkflowService;
  import com.automation.taskplatform.service.WorkflowExecutionService;

  import java.util.Map;

  @RestController
  @RequestMapping("/api/workflows")
  public class WorkflowController {

      private final WorkflowService workflowService;
      private final WorkflowExecutionService workflowExecutionService;

      public WorkflowController(WorkflowService workflowService, WorkflowExecutionService workflowExecutionService) {
          this.workflowService = workflowService;
          this.workflowExecutionService = workflowExecutionService;
      }                                                                                                                                                 
                                                                                                                                                        
      @PostMapping                         // POST /api/workflows                                                                                       
      public ResponseEntity<Workflow> create(@RequestBody Workflow workflow) {                                                                          
          return ResponseEntity.ok(workflowService.createWorkflow(workflow));                                                                           
      }                                                                                                                                                 
                                                                                                                                                        
      @GetMapping                          // GET /api/workflows                                                                                        
      public ResponseEntity<List<Workflow>> getAll() {                                                                                                  
          return ResponseEntity.ok(workflowService.getWorkflowsByUser());                                                                               
      }                                                                                                                                                 
                                                                                                                                                        
      @GetMapping("/{id}")                 // GET /api/workflows/123                                                                                    
      public ResponseEntity<Workflow> getById(@PathVariable Long id) {                                                                                  
          return ResponseEntity.ok(workflowService.getWorkflowById(id));                                                                                
      }                                                                                                                                                 
                                                                                                                                                        
      @PutMapping("/{id}")                 // PUT /api/workflows/123                                                                                    
      public ResponseEntity<Workflow> update(@PathVariable Long id, @RequestBody Workflow workflow) {                                                   
          return ResponseEntity.ok(workflowService.updateWorkflow(id, workflow));                                                                       
      }                                                                                                                                                 
                                                                                                                                                        
      @DeleteMapping("/{id}")              // DELETE /api/workflows/123
      public ResponseEntity<Void> delete(@PathVariable Long id) {
          workflowService.deleteWorkflow(id);
          return ResponseEntity.noContent().build();   // Returns HTTP 204
      }

      @PostMapping("/{id}/run")           // POST /api/workflows/123/run
      public ResponseEntity<?> run(@PathVariable Long id) {
          try {
              workflowExecutionService.runNow(id);
              return ResponseEntity.ok(Map.of("message", "Workflow executed successfully"));
          } catch (Exception e) {
              return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
          }
      }
  }    