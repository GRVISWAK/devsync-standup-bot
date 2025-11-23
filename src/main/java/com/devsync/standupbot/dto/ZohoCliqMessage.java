package com.devsync.standupbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Zoho Cliq message response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZohoCliqMessage {
    private String text;
    private Card card;
    private String bot;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private String title;
        private String theme;
        private List<Button> buttons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Button {
        private String label;
        private String type;
        private String id;
        private Action action;
    }

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String type;
        private ActionData data;
    }

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionData {
        private String name;
    }
}
