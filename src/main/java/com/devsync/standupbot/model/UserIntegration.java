package com.devsync.standupbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing user integration tokens
 * Each user can have their own personal GitHub/Jira tokens
 */
@Entity
@Table(name = "user_integrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "github_personal_token", length = 500)
    private String githubPersonalToken; // Optional: user's personal token

    @Column(name = "jira_account_id")
    private String jiraAccountId;

    @Column(name = "jira_personal_token", length = 500)
    private String jiraPersonalToken; // Optional: user's personal token

    @Column(name = "use_team_tokens")
    private Boolean useTeamTokens; // If true, use team tokens; if false, use personal

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (useTeamTokens == null) {
            useTeamTokens = true; // Default to team tokens
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
