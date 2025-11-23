package com.devsync.standupbot.service;

import com.devsync.standupbot.model.User;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user
     */
    @Transactional
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Find or create user by Zoho user ID
     */
    @Transactional
    public User findOrCreateUser(String zohoUserId, String email, String name) {
        log.info("Finding or creating user with Zoho ID: {}", zohoUserId);
        
        Optional<User> existingUser = userRepository.findByZohoUserId(zohoUserId);
        
        if (existingUser.isPresent()) {
            log.info("User found: {}", existingUser.get().getEmail());
            return existingUser.get();
        }

        // Check by email as fallback
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            user.setZohoUserId(zohoUserId);
            log.info("Updating existing user with Zoho ID: {}", email);
            return userRepository.save(user);
        }

        // Create new user
        User newUser = User.builder()
                .email(email)
                .name(name)
                .zohoUserId(zohoUserId)
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("Created new user: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * Update user's GitHub username
     */
    @Transactional
    public User updateGithubUsername(Long userId, String githubUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setGithubUsername(githubUsername);
        return userRepository.save(user);
    }

    /**
     * Update user's Jira account ID
     */
    @Transactional
    public User updateJiraAccountId(Long userId, String jiraAccountId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setJiraAccountId(jiraAccountId);
        return userRepository.save(user);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by Zoho user ID
     */
    public Optional<User> findByZohoUserId(String zohoUserId) {
        return userRepository.findByZohoUserId(zohoUserId);
    }
}
