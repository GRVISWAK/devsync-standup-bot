package com.devsync.standupbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing team-level configurations
 * Each team can have their own API tokens and settings
 */
@Entity
@Table(name = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String teamName;

    @Column(unique = true)
    private String zohoChannelId;

    @Column(name = "zoho_webhook_url", length = 500)
    private String zohoWebhookUrl;

    @Column(name = "jira_api_url")
    private String jiraApiUrl;

    @Column(name = "jira_email")
    private String jiraEmail;

    @Column(name = "jira_api_token", length = 500)
    private String jiraApiToken;

    @Column(name = "github_org")
    private String githubOrganization;

    @Column(name = "github_token", length = 500)
    private String githubToken;

    @Column(name = "openai_api_key", length = 500)
    private String openaiApiKey;

    @Column(name = "openai_model")
    private String openaiModel;

    @Column(name = "calendar_enabled")
    private Boolean calendarEnabled;

    @Column(name = "reminder_enabled")
    private Boolean reminderEnabled;

    @Column(name = "reminder_time")
    private String reminderTime; // e.g., "09:00"

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (openaiModel == null) {
            openaiModel = "gpt-4";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
