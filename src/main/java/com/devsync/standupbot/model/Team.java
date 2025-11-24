package com.devsync.standupbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing team-level configurations
 * Teams belong to an organization and have a team lead
 */
@Entity
@Table(name = "teams", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"organization_id", "team_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization organization;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "team_lead_zoho_id", nullable = false, length = 100)
    private String teamLeadZohoId;

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

    @Column(name = "timezone")
    private String timezone;

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
        if (timezone == null) {
            timezone = "UTC";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
