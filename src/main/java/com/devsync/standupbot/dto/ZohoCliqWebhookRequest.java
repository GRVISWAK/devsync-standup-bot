package com.devsync.standupbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Zoho Cliq webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZohoCliqWebhookRequest {
    private String name;
    private String user;
    private String text;
    private String command;
    private Arguments arguments;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Arguments {
        private String action;
    }
}
