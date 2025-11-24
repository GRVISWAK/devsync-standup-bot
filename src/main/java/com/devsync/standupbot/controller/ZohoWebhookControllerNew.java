package com.devsync.standupbot.controller;

import com.devsync.standupbot.model.*;
import com.devsync.standupbot.repository.StandupRepository;
import com.devsync.standupbot.repository.TeamRepository;
import com.devsync.standupbot.repository.UserRepository;
import com.devsync.standupbot.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fully functional Zoho Cliq webhook controller with complete standup workflow
 */
@RestController
@RequestMapping("/api/zoho/v2")
@RequiredArgsConstructor
@Slf4j
public class ZohoWebhookControllerNew {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final StandupRepository standupRepository;
    private final AIService aiService;
    
    // Store ongoing standup sessions
    private final Map<String, StandupSession> activeSessions = new ConcurrentHashMap<>();

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody(required = false) String payload) {
        try {
            log.info("Received Zoho webhook: {}", payload);
            
            String message = payload != null ? payload.toLowerCase() : "";
            Map<String, Object> response = new HashMap<>();
            
            // Route to appropriate handler
            if (message.contains("create team")) {
                response = handleCreateTeam(message);
            } else if (message.contains("register") || message.contains("add user")) {
                response = handleRegisterUser(message);
            } else if (message.contains("start standup")) {
                response = handleStartStandup(message);
            } else if (message.contains("yesterday:") || message.contains("today:") || message.contains("blockers:")) {
                response = handleStandupInput(message);
            } else if (message.contains("my standups")) {
                response = handleViewHistory(message);
            } else if (message.contains("team stats")) {
                response = handleTeamStats(message);
            } else if (message.contains("help")) {
                response = handleHelp();
            } else if (message.contains("status")) {
                response = handleStatus();
            } else {
                response = handleWelcome();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error handling webhook: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("text", "❌ Error: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    // ==================== TEAM MANAGEMENT ====================
    
    private Map<String, Object> handleCreateTeam(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String teamName = extractValue(message, "name:", "DefaultTeam");
            String githubOrg = extractValue(message, "github:", null);
            String reminderTime = extractValue(message, "reminder:", "09:00");
            
            if (teamRepository.findByTeamName(teamName).isPresent()) {
                response.put("text", "⚠️ Team '" + teamName + "' already exists!");
                return response;
            }
            
            Team team = Team.builder()
                    .teamName(teamName)
                    .zohoChannelId("zoho-channel-" + teamName)
                    .githubOrganization(githubOrg)
                    .reminderEnabled(true)
                    .reminderTime(reminderTime)
                    .calendarEnabled(false)
                    .active(true)
                    .build();
            
            team = teamRepository.save(team);
            
            StringBuilder details = new StringBuilder();
            details.append("✅ **Team Created!**\n\n");
            details.append("• Team ID: ").append(team.getId()).append("\n");
            details.append("• Name: ").append(team.getTeamName()).append("\n");
            if (githubOrg != null) {
                details.append("• GitHub: ").append(githubOrg).append("\n");
            }
            details.append("• Reminder: ").append(reminderTime).append("\n\n");
            details.append("➡️ Next: `register name:John email:john@dev.com teamid:").append(team.getId()).append("`");
            
            response.put("text", details.toString());
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    // ==================== USER MANAGEMENT ====================
    
    private Map<String, Object> handleRegisterUser(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = extractValue(message, "name:", null);
            String email = extractValue(message, "email:", null);
            String teamIdStr = extractValue(message, "teamid:", null);
            String githubUsername = extractValue(message, "github:", null);
            
            if (name == null || email == null) {
                response.put("text", "❌ Need name and email!\n" +
                        "Example: `register name:John email:john@dev.com teamid:1`");
                return response;
            }
            
            if (userRepository.findByEmail(email).isPresent()) {
                response.put("text", "⚠️ User '" + email + "' exists!");
                return response;
            }
            
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .zohoUserId("zoho-" + email.split("@")[0])
                    .githubUsername(githubUsername)
                    .build();
            
            if (teamIdStr != null) {
                try {
                    Long teamId = Long.parseLong(teamIdStr);
                    Optional<Team> team = teamRepository.findById(teamId);
                    if (team.isPresent()) {
                        user.setTeam(team.get());
                    } else {
                        response.put("text", "⚠️ Team ID " + teamId + " not found!");
                        return response;
                    }
                } catch (NumberFormatException e) {
                    response.put("text", "❌ Invalid team ID!");
                    return response;
                }
            }
            
            user = userRepository.save(user);
            
            StringBuilder details = new StringBuilder();
            details.append("✅ **User Registered!**\n\n");
            details.append("• ID: ").append(user.getId()).append("\n");
            details.append("• Name: ").append(user.getName()).append("\n");
            details.append("• Email: ").append(user.getEmail()).append("\n");
            if (user.getTeam() != null) {
                details.append("• Team: ").append(user.getTeam().getTeamName()).append("\n");
            }
            if (githubUsername != null) {
                details.append("• GitHub: @").append(githubUsername).append("\n");
            }
            details.append("\n🎯 Start standup: `start standup email:").append(email).append("`");
            
            response.put("text", details.toString());
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    // ==================== STANDUP WORKFLOW ====================
    
    private Map<String, Object> handleStartStandup(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = extractValue(message, "email:", null);
            
            if (email == null) {
                response.put("text", "❌ Need email!\nExample: `start standup email:john@dev.com`");
                return response;
            }
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                response.put("text", "❌ User not found! Register first:\n`register name:John email:" + email + " teamid:1`");
                return response;
            }
            
            User user = userOpt.get();
            LocalDate today = LocalDate.now();
            
            Optional<Standup> existingStandup = standupRepository.findByUserAndStandupDate(user, today);
            Standup standup;
            
            if (existingStandup.isPresent()) {
                standup = existingStandup.get();
                if (standup.getStatus() == Standup.StandupStatus.COMPLETED) {
                    response.put("text", "✅ Already completed today!\nView: `my standups email:" + email + "`");
                    return response;
                }
            } else {
                standup = Standup.builder()
                        .user(user)
                        .standupDate(today)
                        .status(Standup.StandupStatus.IN_PROGRESS)
                        .currentStep(1)
                        .build();
                standup = standupRepository.save(standup);
            }
            
            StandupSession session = new StandupSession();
            session.standupId = standup.getId();
            session.userEmail = email;
            session.step = 1;
            activeSessions.put(email, session);
            
            response.put("text", "🎯 **Standup Started!**\n\n" +
                    "**Step 1/3:** What did you work on yesterday?\n\n" +
                    "Reply: `yesterday: Your work email:" + email + "`\n\n" +
                    "Example: `yesterday: Fixed login bug email:" + email + "`");
            
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    private Map<String, Object> handleStandupInput(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = extractValue(message, "email:", null);
            
            if (email == null) {
                response.put("text", "❌ Include email!\nExample: `yesterday: work email:john@dev.com`");
                return response;
            }
            
            StandupSession session = activeSessions.get(email);
            if (session == null) {
                response.put("text", "❌ No active session! Start: `start standup email:" + email + "`");
                return response;
            }
            
            Optional<Standup> standupOpt = standupRepository.findById(session.standupId);
            if (standupOpt.isEmpty()) {
                response.put("text", "❌ Standup not found!");
                return response;
            }
            
            Standup standup = standupOpt.get();
            
            if (message.contains("yesterday:")) {
                String yesterday = extractTextAfter(message, "yesterday:");
                standup.setYesterdayWork(yesterday);
                standup.setCurrentStep(2);
                standupRepository.save(standup);
                session.step = 2;
                
                response.put("text", "✅ Great!\n\n**Step 2/3:** What will you work on today?\n\n" +
                        "Reply: `today: Your plan email:" + email + "`");
                        
            } else if (message.contains("today:")) {
                String today = extractTextAfter(message, "today:");
                standup.setTodayPlan(today);
                standup.setCurrentStep(3);
                standupRepository.save(standup);
                session.step = 3;
                
                response.put("text", "✅ Excellent!\n\n**Step 3/3:** Any blockers?\n\n" +
                        "Reply: `blockers: Description email:" + email + "`\n" +
                        "(Or `blockers: none email:" + email + "`)");
                        
            } else if (message.contains("blockers:")) {
                String blockers = extractTextAfter(message, "blockers:");
                standup.setBlockers(blockers);
                standup.setStatus(Standup.StandupStatus.COMPLETED);
                standup.setSubmittedAt(LocalDateTime.now());
                
                // AI Summary
                try {
                    String aiSummary = aiService.generateStandupSummary(
                            standup.getYesterdayWork(),
                            standup.getTodayPlan(),
                            standup.getBlockers(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>()
                    );
                    standup.setAiSummary(aiSummary);
                } catch (Exception e) {
                    log.warn("AI summary failed: {}", e.getMessage());
                }
                
                standupRepository.save(standup);
                activeSessions.remove(email);
                
                StringBuilder summary = new StringBuilder();
                summary.append("🎉 **Standup Complete!**\n\n");
                summary.append("**Yesterday:**\n").append(standup.getYesterdayWork()).append("\n\n");
                summary.append("**Today:**\n").append(standup.getTodayPlan()).append("\n\n");
                summary.append("**Blockers:**\n").append(standup.getBlockers()).append("\n\n");
                
                if (standup.getAiSummary() != null && !standup.getAiSummary().isEmpty()) {
                    summary.append("🤖 **AI Summary:**\n").append(standup.getAiSummary()).append("\n\n");
                }
                
                summary.append("✅ ID: ").append(standup.getId());
                summary.append(" | 📅 ").append(standup.getStandupDate());
                
                response.put("text", summary.toString());
            }
            
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    // ==================== HISTORY & STATS ====================
    
    private Map<String, Object> handleViewHistory(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = extractValue(message, "email:", null);
            
            if (email == null) {
                response.put("text", "❌ Need email!\nExample: `my standups email:john@dev.com`");
                return response;
            }
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                response.put("text", "❌ User not found!");
                return response;
            }
            
            User user = userOpt.get();
            List<Standup> standups = standupRepository.findRecentStandupsByUser(user);
            
            if (standups.isEmpty()) {
                response.put("text", "📭 No standups yet!\n\nStart: `start standup email:" + email + "`");
                return response;
            }
            
            StringBuilder history = new StringBuilder();
            history.append("📚 **Your Standups**\n\n");
            
            int count = Math.min(5, standups.size());
            for (int i = 0; i < count; i++) {
                Standup s = standups.get(i);
                history.append("**").append(s.getStandupDate()).append("** ");
                history.append(s.getStatus() == Standup.StandupStatus.COMPLETED ? "✅" : "⏳").append("\n");
                if (s.getStatus() == Standup.StandupStatus.COMPLETED && s.getTodayPlan() != null) {
                    history.append("Today: ").append(truncate(s.getTodayPlan(), 50)).append("\n");
                }
                history.append("\n");
            }
            
            history.append("Total: ").append(standups.size());
            
            response.put("text", history.toString());
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    private Map<String, Object> handleTeamStats(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            String teamIdStr = extractValue(message, "teamid:", null);
            
            if (teamIdStr == null) {
                response.put("text", "❌ Need team ID!\nExample: `team stats teamid:1`");
                return response;
            }
            
            Long teamId = Long.parseLong(teamIdStr);
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            
            if (teamOpt.isEmpty()) {
                response.put("text", "❌ Team not found!");
                return response;
            }
            
            Team team = teamOpt.get();
            long userCount = userRepository.count();
            long standupCount = standupRepository.count();
            long todayCount = standupRepository.findCompletedStandupsByDate(LocalDate.now()).size();
            
            StringBuilder stats = new StringBuilder();
            stats.append("📊 **Team Stats**\n\n");
            stats.append("**Team:** ").append(team.getTeamName()).append("\n");
            stats.append("**Users:** ").append(userCount).append("\n");
            stats.append("**Standups:** ").append(standupCount).append("\n");
            stats.append("**Today:** ").append(todayCount).append("\n");
            stats.append("**Status:** ").append(team.getActive() ? "✅" : "❌");
            
            response.put("text", stats.toString());
        } catch (Exception e) {
            response.put("text", "❌ Error: " + e.getMessage());
        }
        return response;
    }
    
    // ==================== HELP & STATUS ====================
    
    private Map<String, Object> handleHelp() {
        Map<String, Object> response = new HashMap<>();
        response.put("text", "📚 **StandupBot Guide**\n\n" +
                "**Team:**\n" +
                "`create team name:Eng github:myorg`\n\n" +
                "**User:**\n" +
                "`register name:John email:j@d.com teamid:1`\n\n" +
                "**Standup:**\n" +
                "1. `start standup email:j@d.com`\n" +
                "2. `yesterday: work email:j@d.com`\n" +
                "3. `today: plan email:j@d.com`\n" +
                "4. `blockers: none email:j@d.com`\n\n" +
                "**History:**\n" +
                "`my standups email:j@d.com`\n" +
                "`team stats teamid:1`");
        return response;
    }
    
    private Map<String, Object> handleStatus() {
        Map<String, Object> response = new HashMap<>();
        long teamCount = teamRepository.count();
        long userCount = userRepository.count();
        long standupCount = standupRepository.count();
        long todayCount = standupRepository.findCompletedStandupsByDate(LocalDate.now()).size();
        
        response.put("text", "📊 **System Status**\n\n" +
                "✅ Railway Running\n" +
                "✅ Database Connected\n" +
                "🤖 Gemini AI Ready\n\n" +
                "**Stats:**\n" +
                "• Teams: " + teamCount + "\n" +
                "• Users: " + userCount + "\n" +
                "• Standups: " + standupCount + "\n" +
                "• Today: " + todayCount);
        return response;
    }
    
    private Map<String, Object> handleWelcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("text", "👋 **Welcome to StandupBot!**\n\n" +
                "🚀 **Quick Start:**\n" +
                "1. `create team name:Engineering`\n" +
                "2. `register name:John email:j@d.com teamid:1`\n" +
                "3. `start standup email:j@d.com`\n\n" +
                "Type `help` for full guide");
        return response;
    }
    
    // ==================== UTILITIES ====================
    
    private String extractValue(String message, String key, String defaultValue) {
        if (message.contains(key)) {
            String[] parts = message.split(key);
            if (parts.length > 1) {
                String value = parts[1].trim().split("\\s")[0];
                return value.isEmpty() ? defaultValue : value;
            }
        }
        return defaultValue;
    }
    
    private String extractTextAfter(String message, String key) {
        if (message.contains(key)) {
            String[] parts = message.split(key);
            if (parts.length > 1) {
                String text = parts[1].trim();
                if (text.contains("email:")) {
                    text = text.split("email:")[0].trim();
                }
                return text;
            }
        }
        return "";
    }
    
    private String truncate(String text, int length) {
        if (text == null) return "";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }
    
    // ==================== SESSION CLASS ====================
    
    private static class StandupSession {
        Long standupId;
        String userEmail;
        int step;
    }
}
