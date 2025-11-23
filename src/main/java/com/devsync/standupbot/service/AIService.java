package com.devsync.standupbot.service;

import com.devsync.standupbot.config.AppConfig;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for AI-powered summary generation using OpenAI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final AppConfig appConfig;

    /**
     * Generate standup summary using OpenAI GPT
     */
    public String generateStandupSummary(String yesterdayWork, String todayPlan, String blockers,
                                        List<String> githubCommits, List<String> jiraTasks,
                                        List<String> calendarEvents) {
        return generateStandupSummary(yesterdayWork, todayPlan, blockers, githubCommits, 
                                     jiraTasks, calendarEvents, appConfig.getOpenaiApiKey(), 
                                     appConfig.getOpenaiModel());
    }

    /**
     * Generate standup summary using OpenAI GPT with specific API key
     */
    public String generateStandupSummary(String yesterdayWork, String todayPlan, String blockers,
                                        List<String> githubCommits, List<String> jiraTasks,
                                        List<String> calendarEvents, String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENAI_API_KEY")) {
            log.warn("OpenAI API key not configured, using fallback summary");
            return generateFallbackSummary(yesterdayWork, todayPlan, blockers);
        }

        try {
            log.info("Generating AI summary for standup");
            
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30));

            StringBuilder prompt = new StringBuilder();
            prompt.append("Generate a concise, professional standup summary based on the following information:\n\n");
            
            prompt.append("**What I did yesterday:**\n").append(yesterdayWork).append("\n\n");
            prompt.append("**What I plan to do today:**\n").append(todayPlan).append("\n\n");
            
            if (blockers != null && !blockers.isEmpty() && !blockers.equalsIgnoreCase("none") 
                && !blockers.equalsIgnoreCase("no blockers")) {
                prompt.append("**Blockers:**\n").append(blockers).append("\n\n");
            }

            if (githubCommits != null && !githubCommits.isEmpty()) {
                prompt.append("**Recent GitHub commits:**\n");
                githubCommits.forEach(commit -> prompt.append("- ").append(commit).append("\n"));
                prompt.append("\n");
            }

            if (jiraTasks != null && !jiraTasks.isEmpty()) {
                prompt.append("**Active Jira tasks:**\n");
                jiraTasks.forEach(task -> prompt.append("- ").append(task).append("\n"));
                prompt.append("\n");
            }

            if (calendarEvents != null && !calendarEvents.isEmpty()) {
                prompt.append("**Upcoming meetings:**\n");
                calendarEvents.forEach(event -> prompt.append("- ").append(event).append("\n"));
                prompt.append("\n");
            }

            prompt.append("Create a brief, engaging summary in 3-5 bullet points that highlights key accomplishments, ");
            prompt.append("plans, and any blockers. Use emojis where appropriate to make it more readable.");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "You are a helpful assistant that creates concise, professional standup summaries for software developers."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(model != null ? model : "gpt-4")
                    .messages(messages)
                    .maxTokens(appConfig.getOpenaiMaxTokens())
                    .temperature(0.7)
                    .build();

            String summary = service.createChatCompletion(completionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.info("AI summary generated successfully");
            service.shutdownExecutor();
            return summary;

        } catch (Exception e) {
            log.error("Error generating AI summary: {}", e.getMessage(), e);
            return generateFallbackSummary(yesterdayWork, todayPlan, blockers);
        }
    }

    /**
     * Fallback summary generation if AI service fails or API key not configured
     * Creates a well-formatted summary without requiring OpenAI
     */
    private String generateFallbackSummary(String yesterdayWork, String todayPlan, String blockers) {
        StringBuilder summary = new StringBuilder();
        summary.append("📋 **Daily Standup Summary**\n\n");
        
        // Yesterday's work
        summary.append("✅ **Completed Yesterday:**\n");
        summary.append(formatBulletPoint(yesterdayWork));
        summary.append("\n");
        
        // Today's plan
        summary.append("🎯 **Plan for Today:**\n");
        summary.append(formatBulletPoint(todayPlan));
        summary.append("\n");
        
        // Blockers (if any)
        if (blockers != null && !blockers.isEmpty() && 
            !blockers.equalsIgnoreCase("none") && 
            !blockers.equalsIgnoreCase("no blockers") &&
            !blockers.equalsIgnoreCase("no")) {
            summary.append("⚠️ **Blockers:**\n");
            summary.append(formatBulletPoint(blockers));
            summary.append("\n");
        } else {
            summary.append("✨ **No blockers reported**\n\n");
        }
        
        summary.append("_Note: Using simplified summary (OpenAI not configured)_");
        
        return summary.toString();
    }
    
    /**
     * Format text as bullet points if not already formatted
     */
    private String formatBulletPoint(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "• _No information provided_\n";
        }
        
        // If already has bullet points, return as is
        if (text.trim().startsWith("•") || text.trim().startsWith("-") || text.trim().startsWith("*")) {
            return text + "\n";
        }
        
        // Add bullet point
        return "• " + text + "\n";
    }
}
