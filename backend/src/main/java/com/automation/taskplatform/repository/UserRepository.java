package com.automation.taskplatform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.automation.taskplatform.model.User;

@Repository // Indicate that this interface is a Spring Data Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Case-insensitive email lookup
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Keep original methods for backwards compatibility
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}