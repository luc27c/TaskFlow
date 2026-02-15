package com.automation.taskplatform.controller;                                                                                                       
                                                                                                                                                        
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.automation.taskplatform.dto.LoginRequest;
import com.automation.taskplatform.dto.SignupRequest;
import com.automation.taskplatform.model.User;
import com.automation.taskplatform.service.AuthService;


                                                                                                                                                        
                                                                                                                                     
@RestController                                                                                                                                       
@RequestMapping("/api/auth")                                                                                                                          
public class AuthController {     // This controller will handle authentication-related endpoints (login, register)                                                                                                                    
                                                                                                                                                        
      private final AuthService authService;                                                                                                            
                                                                                                                                                        
      public AuthController(AuthService authService) {                                                                                                  
          this.authService = authService;                                                                                                               
      }                                                                                                                                                 
                                                                                                                                                        
      @PostMapping("/login")                                                                                                                            
      public ResponseEntity<?> login(@RequestBody LoginRequest request) {                                                                               
          // Call authService.login() and return the result                                                                                             
          return ResponseEntity.ok(authService.login(request));                                                                                         
      }                                                                                                                                                 
                                                                                                                                                        
      @PostMapping("/register")                                                                                                                         
      public ResponseEntity<?> register(@RequestBody SignupRequest request) {                                                                           
          // Build a User from the request, then call authService.register()                                                                            
          User user = new User();                                                                                                                      
          user.setEmail(request.getEmail());                                                                                                           
          user.setPassword(request.getPassword());                                                                                                     
          user.setFirstName(request.getFirstName());                                                                                                   
          user.setLastName(request.getLastName());                                                                                                     
          String token = authService.register(user);                                                                                                   
          return ResponseEntity.ok(token);                                                                                                             
      }                                                                                                                                                 
}