package com.devsync.standupbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Session data for multi-step conversations
 * Used to track user's current conversation state (registration, standup, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    
    /**
     * Zoho user ID (primary key)
     */
    private String zohoUserId;
    
    /**
     * Current conversation state
     */
    private SessionState state;
    
    /**
     * Step within current state
     */
    private int step;
    
    /**
     * Collected data during conversation
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * When session was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Last activity time (for cleanup)
     */
    private LocalDateTime lastActivity;
    
    public enum SessionState {
        IDLE,                   // No active conversation
        REGISTERING_ORG,        // Organization registration flow
        CREATING_TEAM,          // Team creation flow
        ADDING_USER,            // User registration flow
        STANDUP_YESTERDAY,      // Standup: collecting yesterday's work
        STANDUP_TODAY,          // Standup: collecting today's plan
        STANDUP_BLOCKERS,       // Standup: collecting blockers
        UPDATING_GITHUB,        // Updating GitHub credentials
        UPDATING_JIRA           // Updating Jira credentials
    }
    
    /**
     * Store data in session
     */
    public void putData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Get data from session
     */
    public Object getData(String key) {
        return data != null ? data.get(key) : null;
    }
    
    /**
     * Get data with type cast
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = getData(key);
        return value != null ? (T) value : null;
    }
    
    /**
     * Clear session data
     */
    public void reset() {
        this.state = SessionState.IDLE;
        this.step = 0;
        this.data = new HashMap<>();
        this.lastActivity = LocalDateTime.now();
    }
}
