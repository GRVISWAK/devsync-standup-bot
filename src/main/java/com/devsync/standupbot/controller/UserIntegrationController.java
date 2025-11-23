package com.devsync.standupbot.controller;

import com.devsync.standupbot.dto.UserIntegrationRequest;
import com.devsync.standupbot.model.UserIntegration;
import com.devsync.standupbot.service.UserIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for user integration management
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserIntegrationController {

    private final UserIntegrationService userIntegrationService;

    /**
     * Configure user integration
     */
    @PostMapping("/integrations")
    public ResponseEntity<Map<String, Object>> configureIntegration(@RequestBody UserIntegrationRequest request) {
        try {
            log.info("Received integration configuration for user: {}", request.getUserEmail());
            
            UserIntegration integration = userIntegrationService.createOrUpdateIntegration(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Integration configured successfully");
            response.put("integrationId", integration.getId());
            response.put("useTeamTokens", integration.getUseTeamTokens());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error configuring integration: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to configure integration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get setup instructions
     */
    @GetMapping("/integrations/setup-guide")
    public ResponseEntity<Map<String, Object>> getSetupGuide() {
        Map<String, Object> guide = new HashMap<>();
        guide.put("title", "User Integration Setup Guide");
        guide.put("description", "Link your GitHub and Jira accounts to enable automated data fetching");
        
        guide.put("option1", Map.of(
            "name", "Use Team Tokens (Recommended)",
            "description", "Uses your team's configured tokens. Just provide your username/account ID.",
            "required", Map.of(
                "githubUsername", "Your GitHub username",
                "jiraAccountId", "Your Jira account ID (ask your admin)"
            ),
            "example", Map.of(
                "userEmail", "you@company.com",
                "teamId", 1,
                "githubUsername", "yourusername",
                "jiraAccountId", "5f8a1b2c3d4e5f6g",
                "useTeamTokens", true
            )
        ));
        
        guide.put("option2", Map.of(
            "name", "Use Personal Tokens",
            "description", "Use your own GitHub and Jira tokens for API access.",
            "howToGetTokens", Map.of(
                "github", "https://github.com/settings/tokens - Create token with 'repo' and 'user' scopes",
                "jira", "https://id.atlassian.com/manage-profile/security/api-tokens - Create API token"
            ),
            "required", Map.of(
                "githubUsername", "Your GitHub username",
                "githubPersonalToken", "Your GitHub personal access token",
                "jiraAccountId", "Your Jira account ID",
                "jiraPersonalToken", "Your Jira API token"
            ),
            "example", Map.of(
                "userEmail", "you@company.com",
                "teamId", 1,
                "githubUsername", "yourusername",
                "githubPersonalToken", "ghp_your_token_here",
                "jiraAccountId", "5f8a1b2c3d4e5f6g",
                "jiraPersonalToken", "ATATT3xFfGF0_your_token",
                "useTeamTokens", false
            )
        ));
        
        return ResponseEntity.ok(guide);
    }
}
