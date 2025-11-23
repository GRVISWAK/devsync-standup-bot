package com.devsync.standupbot.controller;

import com.devsync.standupbot.exception.ResourceNotFoundException;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.service.TeamService;
import com.devsync.standupbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for user management
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final TeamService teamService;

    /**
     * Create a new user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        log.info("Creating user: {}", request.get("email"));
        
        // Get team
        Long teamId = Long.valueOf(request.get("teamId").toString());
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        
        // Create user
        User user = User.builder()
                .email(request.get("email").toString())
                .name(request.get("name").toString())
                .zohoUserId(request.get("zohoUserId") != null ? request.get("zohoUserId").toString() : null)
                .team(team)
                .role(UserRole.valueOf(request.get("role").toString()))
                .build();
        
        User created = userService.createUser(user);
        
        // Return simplified response to avoid circular references
        Map<String, Object> response = new HashMap<>();
        response.put("id", created.getId());
        response.put("email", created.getEmail());
        response.put("name", created.getName());
        response.put("zohoUserId", created.getZohoUserId());
        response.put("role", created.getRole());
        response.put("teamId", created.getTeam() != null ? created.getTeam().getId() : null);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by email
     */
    @GetMapping("/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("zohoUserId", user.getZohoUserId());
        response.put("role", user.getRole());
        response.put("githubUsername", user.getGithubUsername());
        response.put("jiraAccountId", user.getJiraAccountId());
        response.put("teamId", user.getTeam() != null ? user.getTeam().getId() : null);
        
        return ResponseEntity.ok(response);
    }
}
