package com.devsync.standupbot.controller;

import com.devsync.standupbot.dto.StandupRequest;
import com.devsync.standupbot.dto.StandupResponse;
import com.devsync.standupbot.service.StandupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for standup operations
 */
@RestController
@RequestMapping("/api/standup")
@RequiredArgsConstructor
@Slf4j
public class StandupController {

    private final StandupService standupService;

    /**
     * Start a new standup session
     */
    @PostMapping("/start")
    public ResponseEntity<?> startStandup(@Valid @RequestBody StandupRequest request) {
        log.info("Received request to start standup for user: {}", request.getUserEmail());
        StandupResponse response = standupService.startStandup(request);
        
        // Convert to simple map to avoid serialization issues
        Map<String, Object> result = new HashMap<>();
        result.put("standupId", response.getStandupId());
        result.put("userName", response.getUserName());
        result.put("standupDate", response.getStandupDate() != null ? response.getStandupDate().toString() : null);
        result.put("currentStep", response.getCurrentStep());
        result.put("status", response.getStatus());
        result.put("nextQuestion", response.getNextQuestion());
        
        log.info("Returning standup response successfully");
        return ResponseEntity.ok(result);
    }

    /**
     * Submit standup response for current step
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitResponse(@Valid @RequestBody StandupRequest request) {
        log.info("Received standup response from user: {}", request.getUserEmail());
        StandupResponse response = standupService.processStandupResponse(request);
        
        // Convert to simple map to avoid serialization issues
        Map<String, Object> result = new HashMap<>();
        result.put("standupId", response.getStandupId());
        result.put("userName", response.getUserName());
        result.put("standupDate", response.getStandupDate() != null ? response.getStandupDate().toString() : null);
        result.put("currentStep", response.getCurrentStep());
        result.put("status", response.getStatus());
        result.put("nextQuestion", response.getNextQuestion());
        result.put("yesterdayWork", response.getYesterdayWork());
        result.put("todayPlan", response.getTodayPlan());
        result.put("blockers", response.getBlockers());
        result.put("aiSummary", response.getAiSummary());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get standup by ID
     */
    @GetMapping("/{standupId}")
    public ResponseEntity<?> getStandupById(@PathVariable Long standupId) {
        log.info("Fetching standup with ID: {}", standupId);
        StandupResponse response = standupService.getStandupById(standupId);
        
        // Convert to simple map to avoid serialization issues
        Map<String, Object> result = new HashMap<>();
        result.put("standupId", response.getStandupId());
        result.put("userName", response.getUserName());
        result.put("standupDate", response.getStandupDate() != null ? response.getStandupDate().toString() : null);
        result.put("yesterdayWork", response.getYesterdayWork());
        result.put("todayPlan", response.getTodayPlan());
        result.put("blockers", response.getBlockers());
        result.put("aiSummary", response.getAiSummary());
        result.put("githubCommits", response.getGithubCommits());
        result.put("jiraTasks", response.getJiraTasks());
        result.put("calendarEvents", response.getCalendarEvents());
        result.put("status", response.getStatus());
        result.put("currentStep", response.getCurrentStep());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get user's recent standups
     */
    @GetMapping("/user/{email}")
    public ResponseEntity<List<StandupResponse>> getUserStandups(
            @PathVariable String email,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching standups for user: {}", email);
        List<StandupResponse> standups = standupService.getUserStandups(email, limit);
        return ResponseEntity.ok(standups);
    }

    /**
     * Get standup by date for a user
     */
    @GetMapping("/user/{email}/date/{date}")
    public ResponseEntity<StandupResponse> getStandupByDate(
            @PathVariable String email,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching standup for user {} on date {}", email, date);
        return standupService.getStandupByDate(email, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all completed standups for a specific date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<StandupResponse>> getStandupsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching all standups for date: {}", date);
        List<StandupResponse> standups = standupService.getCompletedStandupsByDate(date);
        return ResponseEntity.ok(standups);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "DevSync Standup Bot");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
