package com.devsync.standupbot.service;

import com.devsync.standupbot.config.AppConfig;
import com.devsync.standupbot.dto.ZohoCliqMessage;
import com.devsync.standupbot.model.Standup;
import com.devsync.standupbot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service for Zoho Cliq integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoCliqService {

    private final AppConfig appConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Send standup summary to Zoho Cliq channel
     */
    public void sendStandupSummary(User user, Standup standup) {
        try {
            // Skip if webhook URL not configured
            String webhookUrl = user.getTeam() != null && user.getTeam().getZohoWebhookUrl() != null 
                ? user.getTeam().getZohoWebhookUrl() 
                : appConfig.getZohoCliqWebhookUrl();
                
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.debug("Zoho Cliq webhook not configured, skipping notification for user: {}", user.getName());
                return;
            }
            
            log.info("Sending standup summary to Zoho Cliq for user: {}", user.getName());

            StringBuilder messageText = new StringBuilder();
            messageText.append("🎯 **Daily Standup - ").append(user.getName()).append("**\n\n");
            
            if (standup.getAiSummary() != null && !standup.getAiSummary().isEmpty()) {
                messageText.append(standup.getAiSummary());
            } else {
                messageText.append("**Yesterday:**\n").append(standup.getYesterdayWork()).append("\n\n");
                messageText.append("**Today:**\n").append(standup.getTodayPlan()).append("\n\n");
                
                if (standup.getBlockers() != null && !standup.getBlockers().isEmpty() 
                    && !standup.getBlockers().equalsIgnoreCase("none")) {
                    messageText.append("**Blockers:**\n").append(standup.getBlockers());
                }
            }

            ZohoCliqMessage message = ZohoCliqMessage.builder()
                    .text(messageText.toString())
                    .bot(appConfig.getZohoCliqBotName())
                    .build();

            WebClient webClient = webClientBuilder
                    .baseUrl(webhookUrl)
                    .build();

            webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> log.info("Successfully sent message to Zoho Cliq"))
                    .doOnError(error -> log.error("Error sending message to Zoho Cliq: {}", error.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error sending standup summary to Zoho Cliq: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a simple text message to Zoho Cliq
     */
    public void sendMessage(String text) {
        try {
            ZohoCliqMessage message = ZohoCliqMessage.builder()
                    .text(text)
                    .bot(appConfig.getZohoCliqBotName())
                    .build();

            WebClient webClient = webClientBuilder
                    .baseUrl(appConfig.getZohoCliqWebhookUrl())
                    .build();

            webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> log.info("Successfully sent message to Zoho Cliq"))
                    .doOnError(error -> log.error("Error sending message: {}", error.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error sending message to Zoho Cliq: {}", e.getMessage(), e);
        }
    }

    /**
     * Send daily standup reminder
     */
    public void sendStandupReminder() {
        String reminderText = "⏰ **Daily Standup Reminder**\n\n" +
                "Good morning team! 👋\n\n" +
                "Time for your daily standup! Use `/standup now` to submit your update.\n\n" +
                "Remember to share:\n" +
                "• What you worked on yesterday\n" +
                "• What you're planning today\n" +
                "• Any blockers you're facing";
        
        sendMessage(reminderText);
    }
}
