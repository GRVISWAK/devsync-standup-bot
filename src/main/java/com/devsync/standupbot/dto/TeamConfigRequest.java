package com.devsync.standupbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for team configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamConfigRequest {
    @NotBlank(message = "Team name is required")
    private String teamName;
    
    private String zohoChannelId;
    private String zohoWebhookUrl;
    
    // Jira Configuration
    private String jiraApiUrl;
    private String jiraEmail;
    private String jiraApiToken;
    
    // GitHub Configuration
    private String githubOrganization;
    private String githubToken;
    
    // OpenAI Configuration
    private String openaiApiKey;
    private String openaiModel;
    
    // Settings
    private Boolean calendarEnabled;
    private Boolean reminderEnabled;
    
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Reminder time must be in HH:mm format (e.g., 09:00)")
    private String reminderTime;
}
