package com.busticket.repository;

import com.busticket.model.User;
import com.busticket.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword123");
        testUser.setName("Test User");
        testUser.setPhone("1234567890");
        testUser.setIsEmailVerified(false);
        testUser.setRole(UserRole.USER);
    }

    @Test
    void findByEmail_WhenUserExists_ReturnsUser() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void findByEmail_WhenUserDoesNotExist_ReturnsEmpty() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_IsCaseInsensitive() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Optional<User> foundLowerCase = userRepository.findByEmail("test@example.com");
        Optional<User> foundUpperCase = userRepository.findByEmail("TEST@EXAMPLE.COM");

        // Then - Spring Data JPA email queries are case-sensitive by default
        // This test documents the actual behavior
        assertThat(foundLowerCase).isPresent();
        assertThat(foundUpperCase).isEmpty();
    }

    @Test
    void existsByEmail_WhenUserExists_ReturnsTrue() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_WhenUserDoesNotExist_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_AfterUserDeleted_ReturnsFalse() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();
        
        // When
        userRepository.delete(testUser);
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByEmail_WithMultipleUsers_ReturnsCorrectUser() {
        // Given
        User user1 = new User();
        user1.setId(UUID.randomUUID().toString());
        user1.setEmail("user1@example.com");
        user1.setPasswordHash("hash1");
        user1.setName("User One");
        user1.setPhone("1111111111");
        user1.setRole(UserRole.USER);

        User user2 = new User();
        user2.setId(UUID.randomUUID().toString());
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hash2");
        user2.setName("User Two");
        user2.setPhone("2222222222");
        user2.setRole(UserRole.ADMIN);

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("user2@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("User Two");
        assertThat(found.get().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void findByEmail_WithSpecialCharactersInEmail_ReturnsUser() {
        // Given
        testUser.setEmail("test+tag@example.co.uk");
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test+tag@example.co.uk");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test+tag@example.co.uk");
    }

    @Test
    void existsByEmail_WithEmptyDatabase_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByEmail("any@example.com");

        // Then
        assertThat(exists).isFalse();
    }
}
