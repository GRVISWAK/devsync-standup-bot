package com.devsync.standupbot.controller;

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
@Slf4j
public class ZohoWebhookController {

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody(required = false) String payload) {
        try {
            log.info("Received Zoho webhook: {}", payload);
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", "👋 Hello! I'm StandupBot.\n\n" +
                    "🎯 Commands:\n" +
                    "• Type 'standup' to start your daily update\n" +
                    "• Type 'help' for more options\n" +
                    "• Type 'status' to see your recent standups");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error handling webhook: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("text", "Sorry, something went wrong. Please try again.");
            return ResponseEntity.ok(errorResponse);
        }
    }
}
