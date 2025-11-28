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
    public ResponseEntity<Map<String, Object>> handleWebhook(
        @RequestBody String payload,
        @RequestHeader Map<String, String> headers) {
        try {
            log.info("Received Zoho webhook payload: {}", payload);
            log.info("Received headers: {}", headers);
            
            ZohoUserContext context = null;
            
            // Try JSON parsing first
            try {
                JsonNode rootNode = objectMapper.readTree(payload);
                context = parseUserContext(rootNode);
                
                // If we successfully parsed JSON but no user info, try to extract from payload/headers
                if (context == null || context.getZohoUserId() == null) {
                    context = extractUserFromPayloadAndHeaders(rootNode, headers);
                }
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
            
            // Check for test user simulation command: "user:2 /register-org"
            if (message.startsWith("user:") && message.contains(" ")) {
                String[] parts = message.split("\\s+", 2);
                String userIdPart = parts[0]; // "user:2"
                String actualMessage = parts[1]; // "/register-org"
                
                if (userIdPart.contains(":")) {
                    String[] userInfo = userIdPart.split(":");
                    if (userInfo.length == 2 && userInfo[0].equals("user")) {
                        String userNum = userInfo[1];
                        log.info("Using test user {} for message: {}", userNum, actualMessage);
                        return ZohoUserContext.builder()
                            .zohoUserId("test_user_" + userNum)
                            .name("Test User " + userNum)
                            .email("testuser" + userNum + "@example.com")
                            .message(actualMessage)
                            .build();
                    }
                }
            }
            
            // Fallback: use default test user
            log.warn("Using default test user for message: {}", message);
            return ZohoUserContext.builder()
                .zohoUserId("test_user_001")
                .name("Test User 1")
                .email("testuser1@example.com")
                .message(message)
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing plain text payload", e);
            return null;
        }
    }
    
    /**
     * Extract user information from Zoho payload and headers
     */
    private ZohoUserContext extractUserFromPayloadAndHeaders(JsonNode rootNode, Map<String, String> headers) {
        try {
            String message = rootNode.path("message").asText("");
            
            // Check for user info in various Zoho webhook formats
            String zohoUserId = null;
            String userName = null;
            String userEmail = null;
            
            // Method 1: Check headers for user information
            zohoUserId = headers.get("x-zoho-user-id");
            if (zohoUserId == null) zohoUserId = headers.get("zoho-user-id");
            
            userName = headers.get("x-zoho-user-name");
            if (userName == null) userName = headers.get("zoho-user-name");
            
            userEmail = headers.get("x-zoho-user-email");
            if (userEmail == null) userEmail = headers.get("zoho-user-email");
            
            // Method 2: Check payload for additional user fields
            if (zohoUserId == null && rootNode.has("actor")) {
                JsonNode actor = rootNode.path("actor");
                zohoUserId = actor.path("id").asText(null);
                userName = actor.path("name").asText(null);
                userEmail = actor.path("email").asText(null);
            }
            
            // Method 3: Check for sender info
            if (zohoUserId == null && rootNode.has("sender")) {
                JsonNode sender = rootNode.path("sender");
                zohoUserId = sender.path("id").asText(null);
                userName = sender.path("name").asText(null);
                userEmail = sender.path("email").asText(null);
            }
            
            // Method 4: Check for from field
            if (zohoUserId == null && rootNode.has("from")) {
                JsonNode from = rootNode.path("from");
                zohoUserId = from.path("id").asText(null);
                userName = from.path("name").asText(null);
                userEmail = from.path("email").asText(null);
            }
            
            // If we found real user data, use it
            if (zohoUserId != null && !zohoUserId.isEmpty()) {
                log.info("Found real user data - ID: {}, Name: {}, Email: {}", zohoUserId, userName, userEmail);
                return ZohoUserContext.builder()
                    .zohoUserId(zohoUserId)
                    .name(userName != null ? userName : "Unknown User")
                    .email(userEmail != null ? userEmail : "unknown@example.com")
                    .message(message)
                    .build();
            }
            
            // Fallback: Use consistent test user based on IP or session
            String clientIp = headers.getOrDefault("x-real-ip", headers.getOrDefault("x-forwarded-for", "unknown"));
            String testUserId = generateConsistentTestUserId(clientIp);
            log.warn("No real user data found, using consistent fallback user: {} (IP: {})", testUserId, clientIp);
            
            return ZohoUserContext.builder()
                .zohoUserId(testUserId)
                .name("Test User")
                .email("test@example.com")
                .message(message)
                .build();
                
        } catch (Exception e) {
            log.error("Error extracting user from payload and headers", e);
            return null;
        }
    }
    
    /**
     * Generate consistent test user ID for development/testing
     */
    private String generateConsistentTestUserId(String clientIp) {
        // For development, use a stable user ID based on IP
        if (clientIp != null && !clientIp.equals("unknown")) {
            // Use last part of IP to create consistent user ID
            String[] ipParts = clientIp.split("\\.");
            if (ipParts.length >= 4) {
                return "test_user_" + ipParts[3];
            }
        }
        
        // Default fallback for consistent testing
        return "test_user_001";
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
