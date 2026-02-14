package com.busticket.repository;

import com.busticket.model.User;
import com.busticket.model.UserRole;
import net.jqwik.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryPropertyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    /**
     * Feature: bus-ticket-website, Property 24: Valid registration creates account
     * Validates: Requirements 5.1
     * 
     * For any valid registration details (unique email, valid password, name, phone),
     * an account should be created successfully.
     */
    @Property(tries = 100)
    void validRegistrationCreatesAccount(
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validName") String name,
            @ForAll("validPhone") String phone) {
        
        // Given - Create a user with valid details
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setName(name);
        user.setPhone(phone);
        user.setIsEmailVerified(false);
        user.setRole(UserRole.USER);

        // When - Save the user
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then - User should be retrievable by email
        Optional<User> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
        assertThat(found.get().getName()).isEqualTo(name);
        assertThat(found.get().getPhone()).isEqualTo(phone);
        
        // Cleanup
        userRepository.delete(savedUser);
        entityManager.flush();
    }

    /**
     * Feature: bus-ticket-website, Property: Email uniqueness enforcement
     * Validates: Requirements 5.1
     * 
     * For any email address, existsByEmail should return true if and only if
     * a user with that email exists in the database.
     */
    @Property(tries = 100)
    void existsByEmailReflectsActualExistence(
            @ForAll("validEmail") String email,
            @ForAll("validName") String name,
            @ForAll("validPhone") String phone) {
        
        // Given - Initially no user exists
        assertThat(userRepository.existsByEmail(email)).isFalse();

        // When - Create and save a user
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setName(name);
        user.setPhone(phone);
        user.setRole(UserRole.USER);
        
        userRepository.save(user);
        entityManager.flush();

        // Then - User should exist
        assertThat(userRepository.existsByEmail(email)).isTrue();
        
        // When - Delete the user
        userRepository.delete(user);
        entityManager.flush();

        // Then - User should not exist
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    /**
     * Feature: bus-ticket-website, Property: Find by email consistency
     * Validates: Requirements 5.2
     * 
     * For any saved user, findByEmail should return the same user data
     * that was originally saved.
     */
    @Property(tries = 100)
    void findByEmailReturnsConsistentData(
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String passwordHash,
            @ForAll("validName") String name,
            @ForAll("validPhone") String phone,
            @ForAll Boolean isEmailVerified,
            @ForAll("userRole") UserRole role) {
        
        // Given - Create a user with specific attributes
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setName(name);
        user.setPhone(phone);
        user.setIsEmailVerified(isEmailVerified);
        user.setRole(role);

        // When - Save and retrieve
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail(email);

        // Then - All attributes should match
        assertThat(found).isPresent();
        User foundUser = found.get();
        assertThat(foundUser.getEmail()).isEqualTo(email);
        assertThat(foundUser.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(foundUser.getName()).isEqualTo(name);
        assertThat(foundUser.getPhone()).isEqualTo(phone);
        assertThat(foundUser.getIsEmailVerified()).isEqualTo(isEmailVerified);
        assertThat(foundUser.getRole()).isEqualTo(role);
        
        // Cleanup
        userRepository.delete(user);
        entityManager.flush();
    }

    /**
     * Feature: bus-ticket-website, Property: Email lookup returns empty for non-existent users
     * Validates: Requirements 5.2
     * 
     * For any email that doesn't exist in the database, findByEmail should return empty.
     */
    @Property(tries = 100)
    void findByEmailReturnsEmptyForNonExistentEmail(@ForAll("validEmail") String email) {
        // Given - Ensure email doesn't exist
        if (userRepository.existsByEmail(email)) {
            return; // Skip this iteration if email happens to exist
        }

        // When
        Optional<User> found = userRepository.findByEmail(email);

        // Then
        assertThat(found).isEmpty();
    }

    // Providers for generating valid test data

    @Provide
    Arbitrary<String> validEmail() {
        Arbitrary<String> localPart = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
        
        Arbitrary<String> domain = Arbitraries.of("example.com", "test.org", "mail.net", "domain.co.uk");
        
        return Combinators.combine(localPart, domain)
                .as((local, dom) -> local + "@" + dom);
    }

    @Provide
    Arbitrary<String> validPassword() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .ofMinLength(8)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> validName() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> validPhone() {
        return Arbitraries.strings()
                .numeric()
                .ofLength(10);
    }

    @Provide
    Arbitrary<UserRole> userRole() {
        return Arbitraries.of(UserRole.values());
    }
}
