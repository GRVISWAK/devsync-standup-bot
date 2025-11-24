package com.devsync.standupbot.controller;

import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.repository.TeamRepository;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller for Zoho Cliq webhook
 */
@RestController
@RequestMapping("/api/zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoWebhookController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody(required = false) String payload) {
        try {
            log.info("Received Zoho webhook: {}", payload);
            
            // Parse the payload to get the message
            String message = payload != null ? payload.toLowerCase() : "";
            
            Map<String, Object> response = new HashMap<>();
            
            // Handle different commands
            if (message.contains("create team")) {
                response = handleCreateTeam(message);
            } else if (message.contains("register") || message.contains("add user")) {
                response = handleRegisterUser(message);
            } else if (message.contains("standup")) {
                response = handleStandupCommand();
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
            errorResponse.put("text", "❌ Sorry, something went wrong: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    private Map<String, Object> handleCreateTeam(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Extract team name (simple parsing - you can improve this)
            String teamName = "DefaultTeam";
            if (message.contains("name:")) {
                teamName = message.split("name:")[1].trim().split("\\s")[0];
            }
            
            // Check if team already exists
            if (teamRepository.findByTeamName(teamName).isPresent()) {
                response.put("text", "⚠️ Team '" + teamName + "' already exists!");
                return response;
            }
            
            Team team = new Team();
            team.setTeamName(teamName);
            team.setZohoChannelId("zoho-channel-" + teamName);
            
            team = teamRepository.save(team);
            
            response.put("text", "✅ Team created successfully!\n\n" +
                    "📋 **Team Details:**\n" +
                    "• Name: " + team.getTeamName() + "\n" +
                    "• ID: " + team.getId() + "\n" +
                    "• Channel: " + team.getZohoChannelId() + "\n\n" +
                    "➡️ Next: Register users with 'register name:yourname email:your@email.com'");
        } catch (Exception e) {
            response.put("text", "❌ Error creating team: " + e.getMessage());
        }
        return response;
    }
    
    private Map<String, Object> handleRegisterUser(String message) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Simple parsing
            String name = "testuser";
            String email = "test@example.com";
            
            if (message.contains("name:")) {
                name = message.split("name:")[1].trim().split("\\s")[0];
            }
            if (message.contains("email:")) {
                email = message.split("email:")[1].trim().split("\\s")[0];
            }
            
            // Check if user exists
            if (userRepository.findByEmail(email).isPresent()) {
                response.put("text", "⚠️ User with email '" + email + "' already exists!");
                return response;
            }
            
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setZohoUserId("zoho-" + name);
            
            user = userRepository.save(user);
            
            response.put("text", "✅ User registered successfully!\n\n" +
                    "👤 **User Details:**\n" +
                    "• Name: " + user.getName() + "\n" +
                    "• Email: " + user.getEmail() + "\n" +
                    "• ID: " + user.getId() + "\n\n" +
                    "➡️ Ready to submit standups! Type 'standup' to begin.");
        } catch (Exception e) {
            response.put("text", "❌ Error registering user: " + e.getMessage());
        }
        return response;
    }
    
    private Map<String, Object> handleStandupCommand() {
        Map<String, Object> response = new HashMap<>();
        response.put("text", "🎯 **Daily Standup**\n\n" +
                "Please provide the following information:\n\n" +
                "1️⃣ **Yesterday's Work:** What did you accomplish?\n" +
                "2️⃣ **Today's Plan:** What will you work on?\n" +
                "3️⃣ **Blockers:** Any obstacles? (or 'none')\n\n" +
                "📝 Format:\n" +
                "```\n" +
                "yesterday: Completed user authentication\n" +
                "today: Working on standup bot integration\n" +
                "blockers: none\n" +
                "```");
        return response;
    }
    
    private Map<String, Object> handleHelp() {
        Map<String, Object> response = new HashMap<>();
        response.put("text", "📚 **StandupBot Help**\n\n" +
                "**Team Management:**\n" +
                "• `create team name:TeamName` - Create a new team\n\n" +
                "**User Management:**\n" +
                "• `register name:john email:john@example.com` - Register user\n\n" +
                "**Standup Commands:**\n" +
                "• `standup` - Start daily standup\n" +
                "• `status` - View recent standups\n\n" +
                "**Other:**\n" +
                "• `help` - Show this help message");
        return response;
    }
    
    private Map<String, Object> handleStatus() {
        Map<String, Object> response = new HashMap<>();
        long teamCount = teamRepository.count();
        long userCount = userRepository.count();
        
        response.put("text", "📊 **System Status**\n\n" +
                "• Teams: " + teamCount + "\n" +
                "• Users: " + userCount + "\n" +
                "• Status: ✅ Running on Railway\n" +
                "• Database: ✅ Connected\n\n" +
                "Type 'help' for available commands.");
        return response;
    }
    
    private Map<String, Object> handleWelcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("text", "👋 Hello! I'm StandupBot.\n\n" +
                "🎯 **Quick Start:**\n" +
                "1. Create team: `create team name:Engineering`\n" +
                "2. Register: `register name:john email:john@example.com`\n" +
                "3. Submit standup: `standup`\n\n" +
                "Type 'help' for all commands.");
        return response;
    }
}
