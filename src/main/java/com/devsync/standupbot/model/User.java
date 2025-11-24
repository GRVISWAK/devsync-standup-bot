package com.devsync.standupbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a user in the system
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_zoho_user_id", columnList = "zoho_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization organization;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "zoho_user_id", unique = true, nullable = false, length = 100)
    private String zohoUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @JsonIgnoreProperties({"users", "hibernateLazyInitializer", "handler"})
    private Team team;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "github_token", length = 500)
    private String githubToken; // Personal Access Token for fetching commits

    @Column(name = "jira_account_id")
    private String jiraAccountId;

    @Column(name = "jira_email")
    private String jiraEmail;

    @Column(name = "jira_api_token", length = 500)
    private String jiraApiToken; // API Token for fetching Jira issues

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (role == null) {
            role = UserRole.DEVELOPER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
