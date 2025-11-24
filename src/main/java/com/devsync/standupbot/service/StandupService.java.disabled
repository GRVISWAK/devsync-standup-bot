package com.devsync.standupbot.service;

import com.devsync.standupbot.config.AppConfig;
import com.devsync.standupbot.dto.StandupRequest;
import com.devsync.standupbot.dto.StandupResponse;
import com.devsync.standupbot.exception.ResourceNotFoundException;
import com.devsync.standupbot.exception.ValidationException;
import com.devsync.standupbot.model.Standup;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.repository.StandupRepository;
import com.devsync.standupbot.repository.UserIntegrationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing standup operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StandupService {

    private final StandupRepository standupRepository;
    private final UserService userService;
    private final AIService aiService;
    private final GitHubService gitHubService;
    private final JiraService jiraService;
    private final GoogleCalendarService googleCalendarService;
    private final ZohoCliqService zohoCliqService;
    private final TeamService teamService;
    private final UserIntegrationRepository userIntegrationRepository;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;

    private static final String[] STANDUP_QUESTIONS = {
        "What did you work on yesterday?",
        "What are you planning to work on today?",
        "Do you have any blockers or challenges?"
    };

    /**
     * Start a new standup session
     */
    @Transactional
    public StandupResponse startStandup(StandupRequest request) {
        log.info("Starting standup for user: {}", request.getUserEmail());

        // Find user by email
        User user = userService.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserEmail()));

        LocalDate today = LocalDate.now();
        
        // Check if standup already exists for today
        Optional<Standup> existingStandup = standupRepository.findByUserAndStandupDate(user, today);
        
        Standup standup;
        if (existingStandup.isPresent() && 
            existingStandup.get().getStatus() == Standup.StandupStatus.IN_PROGRESS) {
            standup = existingStandup.get();
            log.info("Resuming existing standup for user: {}", user.getEmail());
        } else {
            standup = Standup.builder()
                    .user(user)
                    .standupDate(today)
                    .status(Standup.StandupStatus.IN_PROGRESS)
                    .currentStep(1)
                    .build();
            standup = standupRepository.save(standup);
            log.info("Created new standup for user: {}", user.getEmail());
        }

        return buildStandupResponse(standup, STANDUP_QUESTIONS[0]);
    }

    /**
     * Process standup response for a specific step
     */
    @Transactional
    public StandupResponse processStandupResponse(StandupRequest request) {
        log.info("Processing standup response for user: {}", request.getUserEmail());

        User user = userService.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserEmail()));

        LocalDate today = LocalDate.now();
        Standup standup = standupRepository.findByUserAndStandupDate(user, today)
                .orElseThrow(() -> new ValidationException("No active standup session found for today. Please start a standup first."));

        Integer currentStep = standup.getCurrentStep();

        // Store the response based on current step
        switch (currentStep) {
            case 1:
                standup.setYesterdayWork(request.getResponse());
                standup.setCurrentStep(2);
                standupRepository.save(standup);
                return buildStandupResponse(standup, STANDUP_QUESTIONS[1]);

            case 2:
                standup.setTodayPlan(request.getResponse());
                standup.setCurrentStep(3);
                standupRepository.save(standup);
                return buildStandupResponse(standup, STANDUP_QUESTIONS[2]);

            case 3:
                standup.setBlockers(request.getResponse());
                standup.setCurrentStep(4);
                standupRepository.save(standup);
                
                // Trigger async data collection and summary generation
                completeStandupAsync(standup.getId());
                
                return StandupResponse.builder()
                        .standupId(standup.getId())
                        .userName(user.getName())
                        .standupDate(today)
                        .status("PROCESSING")
                        .nextQuestion("Processing your standup... Fetching data from GitHub, Jira, and generating AI summary. You'll receive the summary shortly!")
                        .build();

            default:
                throw new ValidationException("Invalid standup step: " + currentStep);
        }
    }

    /**
     * Complete standup asynchronously - fetch integrations and generate summary
     */
    @Async
    @Transactional
    public void completeStandupAsync(Long standupId) {
        log.info("Completing standup asynchronously: {}", standupId);

        try {
            Standup standup = standupRepository.findById(standupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Standup", standupId));

            User user = standup.getUser();
            Team team = user.getTeam();

            // Get tokens from team or fall back to global config
            String githubToken = team != null && team.getGithubToken() != null ? 
                               team.getGithubToken() : appConfig.getGithubToken();
            String jiraUrl = team != null && team.getJiraApiUrl() != null ? 
                           team.getJiraApiUrl() : appConfig.getJiraApiUrl();
            String jiraEmail = team != null && team.getJiraEmail() != null ? 
                             team.getJiraEmail() : appConfig.getJiraEmail();
            String jiraToken = team != null && team.getJiraApiToken() != null ? 
                             team.getJiraApiToken() : appConfig.getJiraApiToken();
            String openaiKey = team != null && team.getOpenaiApiKey() != null ? 
                             team.getOpenaiApiKey() : appConfig.getOpenaiApiKey();
            String openaiModel = team != null && team.getOpenaiModel() != null ? 
                               team.getOpenaiModel() : appConfig.getOpenaiModel();

            // Fetch data from integrations in parallel with error handling
            CompletableFuture<List<String>> githubFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    if (user.getGithubUsername() != null && !user.getGithubUsername().isEmpty()) {
                        log.info("Fetching GitHub commits for user: {}", user.getEmail());
                        return gitHubService.fetchRecentCommits(user.getGithubUsername(), githubToken);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch GitHub data for user {}: {}", user.getEmail(), e.getMessage());
                }
                return new ArrayList<>();
            });

            CompletableFuture<List<String>> jiraFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    if (user.getJiraAccountId() != null && !user.getJiraAccountId().isEmpty()) {
                        log.info("Fetching Jira tasks for user: {}", user.getEmail());
                        return jiraService.fetchActiveTasks(user.getJiraAccountId(), jiraUrl, jiraEmail, jiraToken);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch Jira data for user {}: {}", user.getEmail(), e.getMessage());
                }
                return new ArrayList<>();
            });

            CompletableFuture<List<String>> calendarFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Fetching calendar events for user: {}", user.getEmail());
                    return googleCalendarService.fetchTodayEvents(user.getEmail());
                } catch (Exception e) {
                    log.warn("Failed to fetch calendar data for user {}: {}", user.getEmail(), e.getMessage());
                }
                return new ArrayList<>();
            });

            // Wait for all futures to complete (gracefully handles failures)
            CompletableFuture.allOf(githubFuture, jiraFuture, calendarFuture).join();

            List<String> githubCommits = githubFuture.join();
            List<String> jiraTasks = jiraFuture.join();
            List<String> calendarEvents = calendarFuture.join();

            // Store integration data
            standup.setGithubCommits(serializeList(githubCommits));
            standup.setJiraTasks(serializeList(jiraTasks));
            standup.setCalendarEvents(serializeList(calendarEvents));

            // Generate AI summary with team's OpenAI key
            String aiSummary = aiService.generateStandupSummary(
                standup.getYesterdayWork(),
                standup.getTodayPlan(),
                standup.getBlockers(),
                githubCommits,
                jiraTasks,
                calendarEvents,
                openaiKey,
                openaiModel
            );

            standup.setAiSummary(aiSummary);
            standup.setStatus(Standup.StandupStatus.COMPLETED);
            standup.setSubmittedAt(java.time.LocalDateTime.now());
            
            standupRepository.save(standup);

            // Send summary to Zoho Cliq
            zohoCliqService.sendStandupSummary(user, standup);

            log.info("Standup completed successfully: {}", standupId);

        } catch (Exception e) {
            log.error("Error completing standup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get user's recent standups
     */
    public List<StandupResponse> getUserStandups(String userEmail, int limit) {
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        List<Standup> standups = standupRepository.findRecentStandupsByUser(user);
        
        return standups.stream()
                .limit(limit)
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get standup by ID
     */
    public StandupResponse getStandupById(Long standupId) {
        Standup standup = standupRepository.findById(standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Standup", standupId));
        return convertToResponse(standup);
    }

    /**
     * Get standup by date
     */
    public Optional<StandupResponse> getStandupByDate(String userEmail, LocalDate date) {
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        return standupRepository.findByUserAndStandupDate(user, date)
                .map(this::convertToResponse);
    }

    /**
     * Get all completed standups for a specific date
     */
    public List<StandupResponse> getCompletedStandupsByDate(LocalDate date) {
        List<Standup> standups = standupRepository.findCompletedStandupsByDate(date);
        return standups.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Build standup response with next question
     */
    private StandupResponse buildStandupResponse(Standup standup, String nextQuestion) {
        String userName = standup.getUser() != null ? standup.getUser().getName() : "Unknown";
        return StandupResponse.builder()
                .standupId(standup.getId())
                .userName(userName)
                .standupDate(standup.getStandupDate())
                .currentStep(standup.getCurrentStep())
                .status(standup.getStatus().name())
                .nextQuestion(nextQuestion)
                .build();
    }

    /**
     * Convert Standup entity to StandupResponse DTO
     */
    private StandupResponse convertToResponse(Standup standup) {
        String userName = standup.getUser() != null ? standup.getUser().getName() : "Unknown";
        return StandupResponse.builder()
                .standupId(standup.getId())
                .userName(userName)
                .standupDate(standup.getStandupDate())
                .yesterdayWork(standup.getYesterdayWork())
                .todayPlan(standup.getTodayPlan())
                .blockers(standup.getBlockers())
                .aiSummary(standup.getAiSummary())
                .githubCommits(deserializeList(standup.getGithubCommits()))
                .jiraTasks(deserializeList(standup.getJiraTasks()))
                .calendarEvents(deserializeList(standup.getCalendarEvents()))
                .status(standup.getStatus().name())
                .currentStep(standup.getCurrentStep())
                .build();
    }

    /**
     * Serialize list to JSON string
     */
    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error serializing list", e);
            return "[]";
        }
    }

    /**
     * Deserialize JSON string to list
     */
    private List<String> deserializeList(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Error deserializing list", e);
            return new ArrayList<>();
        }
    }
}
