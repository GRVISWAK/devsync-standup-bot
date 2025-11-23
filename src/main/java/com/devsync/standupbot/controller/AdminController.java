package com.devsync.standupbot.controller;

import com.devsync.standupbot.dto.TeamConfigRequest;
import com.devsync.standupbot.dto.UserIntegrationRequest;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for team and integration management
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TeamService teamService;

    /**
     * Create or update team configuration
     */
    @PostMapping("/teams")
    public ResponseEntity<Map<String, Object>> createOrUpdateTeam(@Valid @RequestBody TeamConfigRequest request) {
        try {
            log.info("Received team configuration request for: {}", request.getTeamName());
            
            Team team = teamService.createOrUpdateTeam(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team configuration saved successfully");
            response.put("teamId", team.getId());
            response.put("teamName", team.getTeamName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating/updating team: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to save team configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all teams
     */
    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getAllTeams() {
        try {
            List<Team> teams = teamService.getAllTeams();
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            log.error("Error fetching teams: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get team by ID
     */
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<Team> getTeam(@PathVariable Long teamId) {
        try {
            return teamService.getTeamByName(teamId.toString())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching team: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete team
     */
    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Map<String, Object>> deleteTeam(@PathVariable Long teamId) {
        try {
            teamService.deleteTeam(teamId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting team: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete team: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Generate setup guide for team admins
     */
    @GetMapping("/teams/{teamId}/setup-guide")
    public ResponseEntity<Map<String, Object>> getSetupGuide(@PathVariable Long teamId) {
        Map<String, Object> guide = new HashMap<>();
        guide.put("teamId", teamId);
        guide.put("steps", List.of(
            "1. Get OpenAI API key from https://platform.openai.com/",
            "2. Get GitHub Organization token (Settings → Developer settings → Tokens)",
            "3. Get Jira API token from https://id.atlassian.com/manage-profile/security/api-tokens",
            "4. Configure Zoho Cliq bot and get webhook URL",
            "5. POST configuration to /api/admin/teams with all tokens"
        ));
        guide.put("requiredFields", List.of(
            "teamName", "zohoChannelId", "zohoWebhookUrl", 
            "openaiApiKey", "githubToken", "jiraApiToken"
        ));
        
        return ResponseEntity.ok(guide);
    }
}
