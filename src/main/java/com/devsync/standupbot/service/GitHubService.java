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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for GitHub API integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final AppConfig appConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Fetch recent commits for a user
     */
    public List<String> fetchRecentCommits(String username) {
        return fetchRecentCommits(username, appConfig.getGithubToken());
    }

    /**
     * Fetch recent commits for a user with specific token
     */
    public List<String> fetchRecentCommits(String username, String githubToken) {
        if (username == null || username.isEmpty()) {
            log.warn("GitHub username not provided");
            return new ArrayList<>();
        }

        if (githubToken == null || githubToken.isEmpty() || githubToken.equals("YOUR_GITHUB_TOKEN")) {
            log.warn("GitHub token not configured");
            return new ArrayList<>();
        }

        try {
            log.info("Fetching GitHub commits for user: {}", username);

            WebClient webClient = webClientBuilder
                    .baseUrl(appConfig.getGithubApiUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // Get events for the user (last 24 hours)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            
            JsonNode events = webClient.get()
                    .uri("/users/{username}/events", username)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(e -> {
                        log.error("Error fetching GitHub events: {}", e.getMessage());
                        return Mono.just(null);
                    })
                    .block();

            if (events == null || !events.isArray()) {
                return new ArrayList<>();
            }

            List<String> commits = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

            for (JsonNode event : events) {
                if (event.has("type") && "PushEvent".equals(event.get("type").asText())) {
                    String createdAt = event.get("created_at").asText();
                    LocalDateTime eventTime = LocalDateTime.parse(createdAt, formatter);

                    if (eventTime.isAfter(yesterday)) {
                        String repoName = event.get("repo").get("name").asText();
                        JsonNode payload = event.get("payload");
                        
                        if (payload.has("commits")) {
                            for (JsonNode commit : payload.get("commits")) {
                                String message = commit.get("message").asText();
                                commits.add(String.format("%s: %s", repoName, message));
                            }
                        }
                    }
                }

                if (commits.size() >= 5) break; // Limit to 5 commits
            }

            log.info("Found {} GitHub commits for user: {}", commits.size(), username);
            return commits;

        } catch (Exception e) {
            log.error("Error fetching GitHub commits: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
