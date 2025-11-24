package com.devsync.standupbot.controller;

import com.devsync.standupbot.dto.ZohoUserContext;
import com.devsync.standupbot.service.CommandRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Main webhook controller for Zoho Cliq integration
 * Parses JSON payload and routes commands via CommandRouter
 */
@RestController
@RequestMapping("/api/zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoWebhookControllerV3 {
    
    private final CommandRouter commandRouter;
    private final ObjectMapper objectMapper;
    
    /**
     * Main webhook endpoint - handles all Zoho Cliq messages
     * Expected payload:
     * {
     *   "user": {
     *     "id": "12345_67890",
     *     "name": "John Doe",
     *     "email": "john@company.com"
     *   },
     *   "message": "standup",
     *   "channel": {
     *     "id": "channel_123"
     *   }
     * }
     */
    @PostMapping("/v3/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody String payload) {
        try {
            log.info("Received Zoho webhook payload: {}", payload);
            
            ZohoUserContext context = null;
            
            // Try JSON parsing first
            try {
                JsonNode rootNode = objectMapper.readTree(payload);
                context = parseUserContext(rootNode);
            } catch (Exception e) {
                log.info("Not JSON payload, trying plain text format");
            }
            
            // Fallback: Parse plain text format (for basic Zoho Cliq bots)
            if (context == null || context.getZohoUserId() == null) {
                context = parsePlainTextPayload(payload);
            }
            
            if (context == null || context.getZohoUserId() == null) {
                log.error("Failed to parse user context from payload: {}", payload);
                return buildTextResponse("❌ Error: Could not identify user. Please configure bot with user context.\n\n" +
                    "Expected JSON format:\n```\n{\n  \"user\": {\"id\": \"...\", \"name\": \"...\", \"email\": \"...\"},\n  \"message\": \"...\"\n}\n```");
            }
            
            log.info("Processing command from user: {} ({}), message: {}", 
                context.getName(), context.getZohoUserId(), context.getMessage());
            
            // Route command
            String response = commandRouter.routeCommand(context);
            
            // Return formatted response
            return buildTextResponse(response);
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return buildTextResponse("❌ An error occurred: " + e.getMessage() + "\n\nPlease try again or contact support.");
        }
    }
    
    /**
     * Parse Zoho user context from JSON payload
     */
    private ZohoUserContext parseUserContext(JsonNode rootNode) {
        try {
            // Extract user object
            JsonNode userNode = rootNode.path("user");
            if (userNode.isMissingNode()) {
                log.warn("No user object in payload");
                return null;
            }
            
            String zohoUserId = userNode.path("id").asText(null);
            String name = userNode.path("name").asText("Unknown User");
            String email = userNode.path("email").asText(null);
            
            // Extract message
            String message = rootNode.path("message").asText("");
            if (message.isEmpty()) {
                message = rootNode.path("text").asText("");
            }
            
            // Extract channel ID if present
            String channelId = rootNode.path("channel").path("id").asText(null);
            
            return ZohoUserContext.builder()
                .zohoUserId(zohoUserId)
                .name(name)
                .email(email)
                .message(message)
                .channelId(channelId)
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing user context", e);
            return null;
        }
    }
    
    /**
     * Parse plain text payload (fallback for basic Zoho Cliq bots)
     * For testing, accepts format: "user:id:name:email message"
     */
    private ZohoUserContext parsePlainTextPayload(String payload) {
        try {
            // For basic Zoho bots that just send the message text
            // Use a default test user if no user info is provided
            String message = payload.trim();
            
            // Check if payload has user info in format: "userid:username:email message"
            if (message.contains(":") && message.split(":").length >= 3) {
                String[] parts = message.split("\\s+", 2);
                String[] userInfo = parts[0].split(":");
                
                if (userInfo.length >= 3) {
                    return ZohoUserContext.builder()
                        .zohoUserId(userInfo[0])
                        .name(userInfo[1])
                        .email(userInfo[2])
                        .message(parts.length > 1 ? parts[1] : "")
                        .build();
                }
            }
            
            // Fallback: use message as-is with placeholder user
            log.warn("Using placeholder user for plain text message: {}", message);
            return ZohoUserContext.builder()
                .zohoUserId("test_user_001")
                .name("Test User")
                .email("test@example.com")
                .message(message)
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing plain text payload", e);
            return null;
        }
    }
    
    /**
     * Build Zoho Cliq text response
     */
    private ResponseEntity<Map<String, Object>> buildTextResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Build Zoho Cliq card response (for future rich formatting)
     */
    private ResponseEntity<Map<String, Object>> buildCardResponse(String title, String content) {
        Map<String, Object> card = new HashMap<>();
        card.put("title", title);
        card.put("text", content);
        
        Map<String, Object> response = new HashMap<>();
        response.put("card", card);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "DevSync Standup Bot V3");
        response.put("status", "UP");
        response.put("version", "3.0.0 - Organization & Command Router");
        return ResponseEntity.ok(response);
    }
}
