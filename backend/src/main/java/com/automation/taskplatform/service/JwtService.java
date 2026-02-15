package com.automation.taskplatform.service;                                                                                                          
                                                                                                                                                        
  import java.security.Key;
    import java.util.Date;

  import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
  import io.jsonwebtoken.security.Keys;                                                                                                                                
                                                                                                                                                        
  @Service                                                                                                                                              
  public class JwtService {                                                                                                                             
                                                                                                                                                        
      private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);                                                                        
      private final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds                                                                        
                                                                                                                                                        
      public String generateToken(String email) {                                                                                                       
          // TODO: Use Jwts.builder() to create a token                                                                                                 
          // Set: subject(email), issuedAt(now), expiration(now + EXPIRATION_TIME), signWith(secretKey)                                                 
          // Then call .compact() to get the string     
          return Jwts.builder()                                                                                                                             
          .setSubject(email)                                                                                                                            
          .setIssuedAt(new Date())                                                                                                                      
          .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))                                                                        
          .signWith(secretKey)                                                                                                                          
          .compact();                                                                                                               
      }                                                                                                                                                 
                                                                                                                                                        
      public String getUsername(String token) {                                                                                                         
          // TODO: Use Jwts.parserBuilder() to parse the token and extract the subject                                                                  
          return Jwts.parserBuilder()                                                                                                                 
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token) // parses the token
          .getBody() // returns Claims object
          .getSubject();    // subject is the email                                                                                                                           
      }                                                                                                                                                 
                                                                                                                                                        
      public boolean isTokenValid(String token, String email) {                                                                                         
          String tokenEmail = getUsername(token);                                                                                                       
          return tokenEmail.equals(email);                                                                                                              
      }                                                                                                                                                 
  }   