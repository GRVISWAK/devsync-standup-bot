package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions for multi-step conversations
 * Stores session state in memory (ConcurrentHashMap)
 */
@Service
@Slf4j
public class SessionManager {
    
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    
    // Session timeout: 30 minutes of inactivity
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * Get or create session for user
     */
    public UserSession getSession(String zohoUserId) {
        return sessions.computeIfAbsent(zohoUserId, id -> {
            log.info("Creating new session for user: {}", id);
            return UserSession.builder()
                .zohoUserId(id)
                .state(UserSession.SessionState.IDLE)
                .step(0)
                .createdAt(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .build();
        });
    }
    
    /**
     * Update session state
     */
    public void setState(String zohoUserId, UserSession.SessionState state) {
        UserSession session = getSession(zohoUserId);
        session.setState(state);
        session.setStep(0);
        session.setLastActivity(LocalDateTime.now());
        log.info("User {} state changed to: {}", zohoUserId, state);
    }
    
    /**
     * Advance to next step in current state
     */
    public void nextStep(String zohoUserId) {
        UserSession session = getSession(zohoUserId);
        session.setStep(session.getStep() + 1);
        session.setLastActivity(LocalDateTime.now());
    }
    
    /**
     * Store data in session
     */
    public void putData(String zohoUserId, String key, Object value) {
        UserSession session = getSession(zohoUserId);
        session.putData(key, value);
    }
    
    /**
     * Get data from session
     */
    public Object getData(String zohoUserId, String key) {
        UserSession session = getSession(zohoUserId);
        return session.getData(key);
    }
    
    /**
     * Get data with type cast
     */
    public <T> T getData(String zohoUserId, String key, Class<T> type) {
        UserSession session = getSession(zohoUserId);
        return session.getData(key, type);
    }
    
    /**
     * Reset session to IDLE
     */
    public void resetSession(String zohoUserId) {
        UserSession session = sessions.get(zohoUserId);
        if (session != null) {
            session.reset();
            log.info("Session reset for user: {}", zohoUserId);
        }
    }
    
    /**
     * Clear session completely
     */
    public void clearSession(String zohoUserId) {
        sessions.remove(zohoUserId);
        log.info("Session cleared for user: {}", zohoUserId);
    }
    
    /**
     * Check if user has active session
     */
    public boolean hasActiveSession(String zohoUserId) {
        UserSession session = sessions.get(zohoUserId);
        return session != null && session.getState() != UserSession.SessionState.IDLE;
    }
    
    /**
     * Get current state
     */
    public UserSession.SessionState getState(String zohoUserId) {
        UserSession session = sessions.get(zohoUserId);
        return session != null ? session.getState() : UserSession.SessionState.IDLE;
    }
    
    /**
     * Get current step
     */
    public int getStep(String zohoUserId) {
        UserSession session = sessions.get(zohoUserId);
        return session != null ? session.getStep() : 0;
    }
    
    /**
     * Cleanup expired sessions (runs every 10 minutes)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        
        for (Map.Entry<String, UserSession> entry : sessions.entrySet()) {
            UserSession session = entry.getValue();
            if (session.getLastActivity().plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(now)) {
                sessions.remove(entry.getKey());
                removed++;
                log.info("Expired session removed for user: {}", entry.getKey());
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions", removed);
        }
    }
}
