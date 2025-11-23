package com.devsync.standupbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

/**
 * Configuration class for application properties
 */
@Configuration
@Getter
public class AppConfig {

    @Value("${zoho.cliq.webhook.url}")
    private String zohoCliqWebhookUrl;

    @Value("${zoho.cliq.bot.token}")
    private String zohoCliqBotToken;

    @Value("${zoho.cliq.bot.name}")
    private String zohoCliqBotName;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Value("${openai.max.tokens}")
    private Integer openaiMaxTokens;

    @Value("${github.api.url}")
    private String githubApiUrl;

    @Value("${github.token}")
    private String githubToken;

    @Value("${jira.api.url}")
    private String jiraApiUrl;

    @Value("${jira.email}")
    private String jiraEmail;

    @Value("${jira.api.token}")
    private String jiraApiToken;

    @Value("${google.calendar.enabled:false}")
    private Boolean googleCalendarEnabled;

    @Value("${standup.reminder.enabled:true}")
    private Boolean standupReminderEnabled;

    @Value("${standup.timezone:UTC}")
    private String standupTimezone;
}
