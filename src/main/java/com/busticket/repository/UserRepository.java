package com.busticket.repository;

import com.busticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find a user by their email address.
     * 
     * @param email the email address to search for
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user exists with the given email address.
     * 
     * @param email the email address to check
     * @return true if a user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);
}
