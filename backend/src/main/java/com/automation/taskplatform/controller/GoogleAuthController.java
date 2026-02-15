package com.automation.taskplatform.controller;

import com.automation.taskplatform.model.User;
import com.automation.taskplatform.repository.UserRepository;
import com.automation.taskplatform.service.GoogleOAuthService;
import com.automation.taskplatform.service.WorkflowExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final UserRepository userRepository;
    private final WorkflowExecutionService workflowExecutionService;

    public GoogleAuthController(
            GoogleOAuthService googleOAuthService,
            UserRepository userRepository,
            WorkflowExecutionService workflowExecutionService) {
        this.googleOAuthService = googleOAuthService;
        this.userRepository = userRepository;
        this.workflowExecutionService = workflowExecutionService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String authUrl = googleOAuthService.getAuthorizationUrl(email);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String userEmail) {
        try {
            googleOAuthService.handleCallback(code, userEmail);
            // Redirect to frontend with success
            return ResponseEntity.ok(
                "<html><body><script>window.close(); window.opener.postMessage('gmail-connected', '*');</script>" +
                "<h2>Gmail connected successfully! You can close this window.</h2></body></html>"
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                "<html><body><h2>Failed to connect Gmail: " + e.getMessage() + "</h2></body></html>"
            );
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("DEBUG: Looking for user with email: '" + email + "'");

        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> {
                System.out.println("DEBUG: User NOT found for email: '" + email + "'");
                return new RuntimeException("User not found: " + email);
            });

        boolean connected = googleOAuthService.isConnected(user);
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Gmail disconnected"));
    }
}
