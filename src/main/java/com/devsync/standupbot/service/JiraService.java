package com.devsync.standupbot.service;

import com.devsync.standupbot.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for Jira API integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraService {

    private final AppConfig appConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Fetch active Jira tasks for a user
     */
    public List<String> fetchActiveTasks(String accountId) {
        return fetchActiveTasks(accountId, appConfig.getJiraApiUrl(), 
                               appConfig.getJiraEmail(), appConfig.getJiraApiToken());
    }

    /**
     * Fetch active Jira tasks for a user with specific credentials
     */
    public List<String> fetchActiveTasks(String accountId, String jiraUrl, 
                                        String jiraEmail, String jiraToken) {
        if (accountId == null || accountId.isEmpty()) {
            log.warn("Jira account ID not provided");
            return new ArrayList<>();
        }

        if (jiraToken == null || jiraToken.isEmpty() || jiraToken.equals("YOUR_JIRA_TOKEN")) {
            log.warn("Jira credentials not configured");
            return new ArrayList<>();
        }

        try {
            log.info("Fetching Jira tasks for account: {}", accountId);

            String auth = jiraEmail + ":" + jiraToken;
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            WebClient webClient = webClientBuilder
                    .baseUrl(jiraUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // JQL query to get user's active issues
            String jql = String.format(
                "assignee=%s AND status in ('In Progress', 'To Do', 'Open') ORDER BY updated DESC",
                accountId
            );

            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/3/search")
                            .queryParam("jql", jql)
                            .queryParam("maxResults", "5")
                            .queryParam("fields", "summary,status,priority")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(e -> {
                        log.error("Error fetching Jira tasks: {}", e.getMessage());
                        return Mono.just(null);
                    })
                    .block();

            if (response == null || !response.has("issues")) {
                return new ArrayList<>();
            }

            List<String> tasks = new ArrayList<>();
            JsonNode issues = response.get("issues");

            for (JsonNode issue : issues) {
                String key = issue.get("key").asText();
                String summary = issue.get("fields").get("summary").asText();
                String status = issue.get("fields").get("status").get("name").asText();
                
                tasks.add(String.format("[%s] %s - %s", key, summary, status));
            }

            log.info("Found {} Jira tasks for account: {}", tasks.size(), accountId);
            return tasks;

        } catch (Exception e) {
            log.error("Error fetching Jira tasks: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
