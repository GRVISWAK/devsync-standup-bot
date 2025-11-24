package com.devsync.standupbot.service;

import com.devsync.standupbot.model.Organization;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;
    
    /**
     * Register new user to a team (TEAM_LEAD or ORG_ADMIN only)
     */
    @Transactional
    public User registerUser(String adderZohoId, Team team, String newUserZohoId, String newUserName, String newUserEmail) {
        // Check permissions
        if (!permissionService.canAddUserToTeam(adderZohoId, team.getId())) {
            throw new IllegalArgumentException("You don't have permission to add users to this team");
        }
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByZohoUserId(newUserZohoId);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already registered in organization: " + existingUser.get().getOrganization().getName());
        }
        
        // Create user as DEVELOPER
        User user = User.builder()
            .organization(team.getOrganization())
            .team(team)
            .zohoUserId(newUserZohoId)
            .name(newUserName)
            .email(newUserEmail)
            .role(UserRole.DEVELOPER)
            .build();
        
        user = userRepository.save(user);
        log.info("User {} added to team {} as DEVELOPER", newUserName, team.getTeamName());
        
        return user;
    }
    
    /**
     * Update user's GitHub credentials
     */
    @Transactional
    public void updateGitHubCredentials(String zohoUserId, String githubUsername, String githubToken) {
        User user = userRepository.findByZohoUserId(zohoUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setGithubUsername(githubUsername);
        user.setGithubToken(githubToken);
        userRepository.save(user);
        
        log.info("GitHub credentials updated for user {}", user.getName());
    }
    
    /**
     * Update user's Jira credentials
     */
    @Transactional
    public void updateJiraCredentials(String zohoUserId, String jiraAccountId, String jiraEmail, String jiraApiToken) {
        User user = userRepository.findByZohoUserId(zohoUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setJiraAccountId(jiraAccountId);
        user.setJiraEmail(jiraEmail);
        user.setJiraApiToken(jiraApiToken);
        userRepository.save(user);
        
        log.info("Jira credentials updated for user {}", user.getName());
    }
    
    /**
     * Get user by Zoho ID
     */
    public Optional<User> getUserByZohoId(String zohoUserId) {
        return userRepository.findByZohoUserId(zohoUserId);
    }
    
    /**
     * Get all users in team
     */
    public List<User> getUsersByTeam(Long teamId) {
        return userRepository.findByTeamId(teamId);
    }
    
    /**
     * Get all users in organization
     */
    public List<User> getUsersByOrganization(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Check if user is registered
     */
    public boolean isUserRegistered(String zohoUserId) {
        return userRepository.existsByZohoUserId(zohoUserId);
    }

    /**
     * Legacy: Create a new user
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
