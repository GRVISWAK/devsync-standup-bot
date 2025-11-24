package com.devsync.standupbot.service;

import com.devsync.standupbot.config.AppConfig;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            log.warn("AI API key not configured, using fallback summary");
            return generateFallbackSummary(yesterdayWork, todayPlan, blockers);
        }

        // Check if using Gemini API (key starts with "AIza")
        if (apiKey.startsWith("AIza")) {
            return generateGeminiSummary(yesterdayWork, todayPlan, blockers, githubCommits, jiraTasks, calendarEvents, apiKey);
        }

        try {
            log.info("Generating AI summary for standup using OpenAI");
            
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
     * Generate summary using Google Gemini API
     */
    private String generateGeminiSummary(String yesterdayWork, String todayPlan, String blockers,
                                        List<String> githubCommits, List<String> jiraTasks,
                                        List<String> calendarEvents, String apiKey) {
        try {
            log.info("Generating AI summary using Google Gemini");
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("Generate a concise, professional standup summary based on the following information:\n\n");
            
            prompt.append("What I did yesterday: ").append(yesterdayWork).append("\n\n");
            prompt.append("What I plan to do today: ").append(todayPlan).append("\n\n");
            
            if (blockers != null && !blockers.isEmpty() && !blockers.equalsIgnoreCase("none") 
                && !blockers.equalsIgnoreCase("no blockers")) {
                prompt.append("Blockers: ").append(blockers).append("\n\n");
            }

            if (githubCommits != null && !githubCommits.isEmpty()) {
                prompt.append("Recent GitHub commits:\n");
                githubCommits.forEach(commit -> prompt.append("- ").append(commit).append("\n"));
                prompt.append("\n");
            }

            if (jiraTasks != null && !jiraTasks.isEmpty()) {
                prompt.append("Active Jira tasks:\n");
                jiraTasks.forEach(task -> prompt.append("- ").append(task).append("\n"));
                prompt.append("\n");
            }

            if (calendarEvents != null && !calendarEvents.isEmpty()) {
                prompt.append("Upcoming meetings:\n");
                calendarEvents.forEach(event -> prompt.append("- ").append(event).append("\n"));
                prompt.append("\n");
            }

            prompt.append("Create a brief, engaging summary in 3-5 bullet points that highlights key accomplishments, ");
            prompt.append("plans, and any blockers. Use emojis where appropriate to make it more readable.");

            // Call Gemini API
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt.toString());
            parts.add(part);
            content.put("parts", parts);
            requestBody.put("contents", List.of(content));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidate = candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> partsResponse = (List<Map<String, String>>) contentResponse.get("parts");
                    if (!partsResponse.isEmpty()) {
                        String summary = partsResponse.get(0).get("text");
                        log.info("Gemini AI summary generated successfully");
                        return summary;
                    }
                }
            }
            
            log.warn("Gemini API returned unexpected response, using fallback");
            return generateFallbackSummary(yesterdayWork, todayPlan, blockers);
            
        } catch (Exception e) {
            log.error("Error generating Gemini AI summary: {}", e.getMessage(), e);
            return generateFallbackSummary(yesterdayWork, todayPlan, blockers);
        }
    }

    /**
     * Fallback summary generation if AI service fails or API key not configured
     * Creates a well-formatted summary without requiring AI
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
