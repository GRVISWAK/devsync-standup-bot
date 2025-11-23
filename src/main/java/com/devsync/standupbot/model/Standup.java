package com.devsync.standupbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a standup entry
 */
@Entity
@Table(name = "standups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Standup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"team", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "standup_date", nullable = false)
    private LocalDate standupDate;

    @Column(name = "yesterday_work", columnDefinition = "TEXT")
    private String yesterdayWork;

    @Column(name = "today_plan", columnDefinition = "TEXT")
    private String todayPlan;

    @Column(columnDefinition = "TEXT")
    private String blockers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StandupStatus status;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "github_commits", columnDefinition = "TEXT")
    private String githubCommits;

    @Column(name = "jira_tasks", columnDefinition = "TEXT")
    private String jiraTasks;

    @Column(name = "calendar_events", columnDefinition = "TEXT")
    private String calendarEvents;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = StandupStatus.IN_PROGRESS;
        }
        if (currentStep == null) {
            currentStep = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for standup status
     */
    public enum StandupStatus {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
