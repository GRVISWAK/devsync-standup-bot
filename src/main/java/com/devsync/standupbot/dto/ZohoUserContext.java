package com.devsync.standupbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Zoho Cliq user context extracted from webhook payload
 * Zoho sends: {"user": {"id": "zoho_12345", "name": "John Doe", "email": "john@company.com"}, "message": "..."}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZohoUserContext {
    
    /**
     * Unique Zoho user ID - PRIMARY IDENTIFIER
     * Example: "12345_67890" or "zoho_user_abc123"
     */
    private String zohoUserId;
    
    /**
     * User's display name from Zoho Cliq
     * Example: "John Doe"
     */
    private String name;
    
    /**
     * User's email from Zoho Cliq
     * Example: "john.doe@company.com"
     */
    private String email;
    
    /**
     * Zoho channel ID where message was sent (for group chats)
     */
    private String channelId;
    
    /**
     * The actual message/command sent by user
     * Example: "/register-org" or "standup"
     */
    private String message;
}
