package com.automation.taskplatform.service;                                                                                                          
                                                                                                                                                        
import java.util.Optional;

  import org.springframework.stereotype.Service;

  import com.automation.taskplatform.dto.LoginRequest;
  import com.automation.taskplatform.dto.LoginResponse;
  import com.automation.taskplatform.model.User;
  import com.automation.taskplatform.repository.UserRepository;  
  import org.springframework.security.crypto.password.PasswordEncoder;                                                                                                     
                                                                                                                                                        
  @Service                                                                                                                                              
  public class AuthService {                                                                                                                            
                                                                                                                                                        
      private final UserRepository userRepository;                                                                                                      
      private final JwtService jwtService;     
      private final PasswordEncoder passwordEncoder;                                                                                                         
                                                                                                                                                        
      public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {                                                                        
          this.userRepository = userRepository;                                                                                                         
          this.jwtService = jwtService;     
          this.passwordEncoder = passwordEncoder;                                                                                                       
      }

      public LoginResponse login(LoginRequest request) {                                                                                                    
        // 1. Find user                                                                                                                                   
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(request.getEmail());                                                                          
                                                                                                                                                            
        // 2. Check if user exists                                                                                                                        
        if (!userOpt.isPresent()) {    // if userOpt is empty then user not found                                                                                                                                   
            throw new RuntimeException("User not found");                                                                                                 
        }                                                                                                                                                 
                                                                                                                                                            
        User user = userOpt.get();    

        // 3. Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

                                                                                                                                                    
                                                                                                                                                            
        // 4. Generate token                                                                                                                              
        String token = jwtService.generateToken(user.getEmail());                                                                                                    
                                                                                                                                                            
        // 5. Return response                                                                                                                             
        return new LoginResponse(token, user.getEmail(), user.getFirstName(), user.getLastName());                                                        
    }   
    
    public String register(User user) {
        // Check if user with the same email already exists
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save the new user
        User savedUser = userRepository.save(user);
        

        // Generate token for the new user
        String token = jwtService.generateToken(savedUser.getEmail());

        return token;
    }
  } 