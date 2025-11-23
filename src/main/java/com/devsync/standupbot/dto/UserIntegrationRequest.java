package com.devsync.standupbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user integration configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIntegrationRequest {
    private String userEmail;
    private Long teamId;
    
    // GitHub Configuration
    private String githubUsername;
    private String githubPersonalToken; // Optional
    
    // Jira Configuration
    private String jiraAccountId;
    private String jiraPersonalToken; // Optional
    
    // Use team tokens or personal tokens
    private Boolean useTeamTokens;
}
