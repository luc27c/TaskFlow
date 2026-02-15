package com.automation.taskplatform.service;

import com.automation.taskplatform.model.User;
import com.automation.taskplatform.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final UserRepository userRepository;

    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/gmail.send"
    );

    public GoogleOAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getAuthorizationUrl(String userEmail) {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            clientId,
            clientSecret,
            SCOPES
        )
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .build();

        return flow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            .setState(userEmail)  // Pass user email as state
            .build();
    }

    public void handleCallback(String code, String userEmail) throws IOException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            "https://oauth2.googleapis.com/token",
            clientId,
            clientSecret,
            code,
            redirectUri
        ).execute();

        // Save tokens to user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setGoogleAccessToken(tokenResponse.getAccessToken());
        user.setGoogleRefreshToken(tokenResponse.getRefreshToken());
        user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));

        userRepository.save(user);
    }

    public String getAccessToken(User user) throws IOException {
        // Check if token is expired or about to expire (within 5 minutes)
        if (user.getGoogleTokenExpiry() != null &&
            user.getGoogleTokenExpiry().isBefore(LocalDateTime.now().plusMinutes(5))) {
            log.info("Access token expired or expiring soon for user {}, refreshing...", user.getEmail());
            refreshAccessToken(user);
        }
        return user.getGoogleAccessToken();
    }

    private void refreshAccessToken(User user) throws IOException {
        if (user.getGoogleRefreshToken() == null) {
            throw new IOException("No refresh token available for user " + user.getEmail());
        }

        log.info("Refreshing Google access token for user: {}", user.getEmail());

        GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            user.getGoogleRefreshToken(),
            clientId,
            clientSecret
        ).execute();

        user.setGoogleAccessToken(tokenResponse.getAccessToken());
        user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        userRepository.save(user);

        log.info("Successfully refreshed access token for user: {}", user.getEmail());
    }

    public boolean isConnected(User user) {
        return user.getGoogleRefreshToken() != null;
    }
}
