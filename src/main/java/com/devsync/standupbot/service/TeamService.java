package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.TeamConfigRequest;
import com.devsync.standupbot.model.Organization;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.repository.TeamRepository;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing teams and their configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    /**
     * Create new team (ORG_ADMIN only)
     * Team creator becomes TEAM_LEAD
     */
    @Transactional
    public Team createTeam(String creatorZohoId, String teamName, String githubOrg, String zohoChannelId) {
        // Get user and verify permissions
        User creator = userRepository.findByZohoUserId(creatorZohoId)
            .orElseThrow(() -> new IllegalArgumentException("User not registered. Please register organization first with /register-org"));
        
        Organization organization = creator.getOrganization();
        
        // Only ORG_ADMIN can create teams
        if (!permissionService.canCreateTeam(creatorZohoId, organization.getId())) {
            throw new IllegalArgumentException("Only organization admins can create teams");
        }
        
        // Check if team already exists in this organization
        Optional<Team> existingTeam = teamRepository.findByOrganizationAndTeamName(organization, teamName);
        if (existingTeam.isPresent()) {
            throw new IllegalArgumentException("Team '" + teamName + "' already exists in your organization");
        }
        
        // Create team
        Team team = Team.builder()
            .organization(organization)
            .teamName(teamName)
            .teamLeadZohoId(creatorZohoId)
            .githubOrganization(githubOrg)
            .zohoChannelId(zohoChannelId)
            .active(true)
            .build();
        
        team = teamRepository.save(team);
        log.info("Team created: {} in organization {} by {}", teamName, organization.getName(), creator.getName());
        
        // Update creator to TEAM_LEAD and assign to team
        creator.setRole(UserRole.TEAM_LEAD);
        creator.setTeam(team);
        userRepository.save(creator);
        log.info("User {} promoted to TEAM_LEAD of team {}", creator.getName(), teamName);
        
        return team;
    }
    
    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(Long teamId) {
        return teamRepository.findById(teamId);
    }
    
    /**
     * Get all teams in organization
     */
    public List<Team> getTeamsByOrganization(Organization organization) {
        return teamRepository.findByOrganization(organization);
    }
    
    /**
     * Update team GitHub token
     */
    @Transactional
    public void updateGitHubToken(Long teamId, String zohoUserId, String githubToken) {
        if (!permissionService.canManageTeam(zohoUserId, teamId)) {
            throw new IllegalArgumentException("You don't have permission to manage this team");
        }
        
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        team.setGithubToken(githubToken);
        teamRepository.save(team);
        log.info("GitHub token updated for team {}", team.getTeamName());
    }
    
    /**
     * Update team Jira credentials
     */
    @Transactional
    public void updateJiraCredentials(Long teamId, String zohoUserId, String jiraApiUrl, String jiraEmail, String jiraApiToken) {
        if (!permissionService.canManageTeam(zohoUserId, teamId)) {
            throw new IllegalArgumentException("You don't have permission to manage this team");
        }
        
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        team.setJiraApiUrl(jiraApiUrl);
        team.setJiraEmail(jiraEmail);
        team.setJiraApiToken(jiraApiToken);
        teamRepository.save(team);
        log.info("Jira credentials updated for team {}", team.getTeamName());
    }

    /**
     * Legacy method for backward compatibility
     * Create or update team configuration
     */
    @Transactional
    public Team createOrUpdateTeam(TeamConfigRequest request) {
        log.info("Creating or updating team: {}", request.getTeamName());

        Optional<Team> existingTeam = teamRepository.findByTeamName(request.getTeamName());

        Team team;
        if (existingTeam.isPresent()) {
            team = existingTeam.get();
            log.info("Updating existing team: {}", request.getTeamName());
        } else {
            team = new Team();
            team.setTeamName(request.getTeamName());
            log.info("Creating new team: {}", request.getTeamName());
        }

        // Update team configuration
        team.setZohoChannelId(request.getZohoChannelId());
        team.setZohoWebhookUrl(request.getZohoWebhookUrl());
        team.setJiraApiUrl(request.getJiraApiUrl());
        team.setJiraEmail(request.getJiraEmail());
        team.setJiraApiToken(request.getJiraApiToken());
        team.setGithubOrganization(request.getGithubOrganization());
        team.setGithubToken(request.getGithubToken());
        team.setOpenaiApiKey(request.getOpenaiApiKey());
        team.setOpenaiModel(request.getOpenaiModel());
        team.setCalendarEnabled(request.getCalendarEnabled());
        team.setReminderEnabled(request.getReminderEnabled());
        team.setReminderTime(request.getReminderTime());

        return teamRepository.save(team);
    }

    /**
     * Get team by name
     */
    public Optional<Team> getTeamByName(String teamName) {
        return teamRepository.findByTeamName(teamName);
    }

    /**
     * Get team by Zoho channel ID
     */
    public Optional<Team> getTeamByZohoChannelId(String channelId) {
        return teamRepository.findByZohoChannelId(channelId);
    }

    /**
     * Get all teams
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Delete team
     */
    @Transactional
    public void deleteTeam(Long teamId) {
        teamRepository.deleteById(teamId);
        log.info("Deleted team with ID: {}", teamId);
    }

    /**
     * Get GitHub token for team (falls back to global config if not set)
     */
    public String getGithubToken(Team team, String globalToken) {
        return team.getGithubToken() != null ? team.getGithubToken() : globalToken;
    }

    /**
     * Get Jira configuration for team
     */
    public boolean hasJiraConfig(Team team) {
        return team.getJiraApiToken() != null && team.getJiraApiUrl() != null;
    }

    /**
     * Get OpenAI key for team (falls back to global config if not set)
     */
    public String getOpenAiKey(Team team, String globalKey) {
        return team.getOpenaiApiKey() != null ? team.getOpenaiApiKey() : globalKey;
    }
}
