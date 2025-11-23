package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.TeamConfigRequest;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.repository.TeamRepository;
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

    /**
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
     * Get team by ID
     */
    public Optional<Team> getTeamById(Long teamId) {
        return teamRepository.findById(teamId);
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
