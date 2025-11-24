package com.devsync.standupbot.controller;

import com.devsync.standupbot.dto.StandupRequest;
import com.devsync.standupbot.dto.StandupResponse;
import com.devsync.standupbot.dto.ZohoCliqMessage;
import com.devsync.standupbot.dto.ZohoCliqWebhookRequest;
import com.devsync.standupbot.service.StandupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling Zoho Cliq webhook requests
 */
@RestController
@RequestMapping("/api/webhook/cliq")
@RequiredArgsConstructor
@Slf4j
public class ZohoCliqWebhookController {

    private final StandupService standupService;

    /**
     * Handle Zoho Cliq slash commands
     */
    @PostMapping("/command")
    public ResponseEntity<ZohoCliqMessage> handleCommand(@RequestBody ZohoCliqWebhookRequest request) {
        try {
            log.info("Received Zoho Cliq command: {} from user: {}", request.getText(), request.getName());

            String command = request.getText() != null ? request.getText().toLowerCase() : "";
            String userName = request.getName();
            String userId = request.getUser();

            // Extract email from Zoho user info (you might need to adjust this based on actual payload)
            String userEmail = userId + "@your-domain.com"; // Adjust based on your setup

            ZohoCliqMessage response;

            if (command.contains("standup now") || command.equals("/standup")) {
                response = handleStartStandup(userId, userEmail, userName);
            } else if (command.contains("myupdates") || command.contains("my updates")) {
                response = handleMyUpdates(userEmail);
            } else if (command.contains("help")) {
                response = handleHelp();
            } else {
                response = ZohoCliqMessage.builder()
                        .text("Unknown command. Type `/standup help` for available commands.")
                        .build();
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error handling Zoho Cliq command: {}", e.getMessage(), e);
            ZohoCliqMessage errorResponse = ZohoCliqMessage.builder()
                    .text("❌ An error occurred while processing your request. Please try again.")
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle standup responses (message handler)
     */
    @PostMapping("/message")
    public ResponseEntity<ZohoCliqMessage> handleMessage(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received message from Zoho Cliq: {}", payload);

            // Extract user info and message
            String message = (String) payload.get("text");
            Map<String, Object> userInfo = (Map<String, Object>) payload.get("user");
            String userEmail = (String) userInfo.get("email");
            String userName = (String) userInfo.get("name");
            String userId = (String) userInfo.get("id");

            // Process the message as a standup response
            StandupRequest standupRequest = StandupRequest.builder()
                    .zohoUserId(userId)
                    .userEmail(userEmail)
                    .userName(userName)
                    .response(message)
                    .build();

            StandupResponse standupResponse = standupService.processStandupResponse(standupRequest);

            ZohoCliqMessage cliqMessage = ZohoCliqMessage.builder()
                    .text(standupResponse.getNextQuestion() != null ? 
                          standupResponse.getNextQuestion() : 
                          "✅ Response recorded!")
                    .build();

            return ResponseEntity.ok(cliqMessage);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            ZohoCliqMessage errorResponse = ZohoCliqMessage.builder()
                    .text("❌ Error processing your message. Please try again.")
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle start standup command
     */
    private ZohoCliqMessage handleStartStandup(String userId, String userEmail, String userName) {
        StandupRequest request = StandupRequest.builder()
                .zohoUserId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .build();

        StandupResponse response = standupService.startStandup(request);

        return ZohoCliqMessage.builder()
                .text("🚀 **Daily Standup Started!**\n\n" + 
                      "**Step " + response.getCurrentStep() + "/3**\n\n" +
                      response.getNextQuestion() + "\n\n" +
                      "_Reply with your answer to continue..._")
                .build();
    }

    /**
     * Handle my updates command
     */
    private ZohoCliqMessage handleMyUpdates(String userEmail) {
        var standups = standupService.getUserStandups(userEmail, 5);

        if (standups.isEmpty()) {
            return ZohoCliqMessage.builder()
                    .text("📋 You don't have any standup updates yet.\n\n" +
                          "Use `/standup now` to create your first standup!")
                    .build();
        }

        StringBuilder message = new StringBuilder("📋 **Your Recent Standups**\n\n");
        
        standups.forEach(standup -> {
            message.append("**").append(standup.getStandupDate()).append("**\n");
            if (standup.getAiSummary() != null && !standup.getAiSummary().isEmpty()) {
                message.append(standup.getAiSummary()).append("\n\n");
            } else {
                message.append("Status: ").append(standup.getStatus()).append("\n\n");
            }
            message.append("---\n\n");
        });

        return ZohoCliqMessage.builder()
                .text(message.toString())
                .build();
    }

    /**
     * Handle help command
     */
    private ZohoCliqMessage handleHelp() {
        String helpText = """
                📖 **DevSync Standup Bot - Help**
                
                **Available Commands:**
                
                `/standup now` or `/standup`
                Start a new daily standup session
                
                `/myupdates` or `/my updates`
                View your recent standup updates
                
                `/standup help`
                Show this help message
                
                **How it works:**
                1. Use `/standup now` to begin
                2. Answer three questions:
                   • What you did yesterday
                   • What you plan today
                   • Any blockers
                3. The bot will fetch your GitHub commits and Jira tasks
                4. An AI summary will be generated and posted to the channel
                
                **Integrations:**
                • GitHub - Recent commits
                • Jira - Active tasks
                • Google Calendar - Today's events (if configured)
                • OpenAI - AI-powered summaries
                """;

        return ZohoCliqMessage.builder()
                .text(helpText)
                .build();
    }
}
