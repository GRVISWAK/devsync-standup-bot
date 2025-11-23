package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.UserIntegrationRequest;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserIntegration;
import com.devsync.standupbot.repository.UserIntegrationRepository;
import com.devsync.standupbot.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing user integrations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserIntegrationService {

    private final UserIntegrationRepository userIntegrationRepository;
    private final UserService userService;
    private final TeamRepository teamRepository;

    /**
     * Create or update user integration
     */
    @Transactional
    public UserIntegration createOrUpdateIntegration(UserIntegrationRequest request) {
        log.info("Creating or updating integration for user: {}", request.getUserEmail());

        User user = userService.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserEmail()));

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found: " + request.getTeamId()));
        }

        Optional<UserIntegration> existing = userIntegrationRepository.findByUser(user);

        UserIntegration integration;
        if (existing.isPresent()) {
            integration = existing.get();
            log.info("Updating existing integration for user: {}", user.getEmail());
        } else {
            integration = new UserIntegration();
            integration.setUser(user);
            log.info("Creating new integration for user: {}", user.getEmail());
        }

        integration.setTeam(team);
        integration.setGithubUsername(request.getGithubUsername());
        integration.setGithubPersonalToken(request.getGithubPersonalToken());
        integration.setJiraAccountId(request.getJiraAccountId());
        integration.setJiraPersonalToken(request.getJiraPersonalToken());
        integration.setUseTeamTokens(request.getUseTeamTokens());

        // Also update User entity for backward compatibility
        user.setGithubUsername(request.getGithubUsername());
        user.setJiraAccountId(request.getJiraAccountId());
        user.setTeam(team);
        userService.findByEmail(user.getEmail()); // This will trigger save

        return userIntegrationRepository.save(integration);
    }

    /**
     * Get user integration
     */
    public Optional<UserIntegration> getUserIntegration(User user) {
        return userIntegrationRepository.findByUser(user);
    }

    /**
     * Get GitHub token for user (personal or team)
     */
    public String getGithubToken(User user, String teamToken) {
        Optional<UserIntegration> integration = userIntegrationRepository.findByUser(user);
        
        if (integration.isPresent()) {
            UserIntegration ui = integration.get();
            // Use personal token if available and not using team tokens
            if (!ui.getUseTeamTokens() && ui.getGithubPersonalToken() != null) {
                return ui.getGithubPersonalToken();
            }
        }
        
        // Fall back to team token
        return teamToken;
    }

    /**
     * Get Jira token for user (personal or team)
     */
    public String getJiraToken(User user, String teamToken) {
        Optional<UserIntegration> integration = userIntegrationRepository.findByUser(user);
        
        if (integration.isPresent()) {
            UserIntegration ui = integration.get();
            // Use personal token if available and not using team tokens
            if (!ui.getUseTeamTokens() && ui.getJiraPersonalToken() != null) {
                return ui.getJiraPersonalToken();
            }
        }
        
        // Fall back to team token
        return teamToken;
    }
}
