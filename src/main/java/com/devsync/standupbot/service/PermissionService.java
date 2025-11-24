package com.devsync.standupbot.service;

import com.devsync.standupbot.model.Organization;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.repository.OrganizationRepository;
import com.devsync.standupbot.repository.TeamRepository;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for handling role-based permissions
 * Implements access control for Organization → Team → User hierarchy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Check if user can manage organization (org admin only)
     */
    public boolean canManageOrganization(String zohoUserId, Long organizationId) {
        return getUserByZohoId(zohoUserId)
            .map(user -> user.getOrganization().getId().equals(organizationId) 
                && user.getRole() == UserRole.ORG_ADMIN)
            .orElse(false);
    }

    /**
     * Check if user can create teams in organization (org admin only)
     */
    public boolean canCreateTeam(String zohoUserId, Long organizationId) {
        return canManageOrganization(zohoUserId, organizationId);
    }

    /**
     * Check if user can manage team (org admin or team lead of that team)
     */
    public boolean canManageTeam(String zohoUserId, Long teamId) {
        Optional<User> userOpt = getUserByZohoId(zohoUserId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        
        // Org admins can manage any team in their org
        if (user.getRole() == UserRole.ORG_ADMIN) {
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            return teamOpt.isPresent() 
                && teamOpt.get().getOrganization().getId().equals(user.getOrganization().getId());
        }

        // Team leads can only manage their own team
        if (user.getRole() == UserRole.TEAM_LEAD) {
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            return teamOpt.isPresent() 
                && teamOpt.get().getTeamLeadZohoId().equals(zohoUserId);
        }

        return false;
    }

    /**
     * Check if user can add users to team (org admin or team lead)
     */
    public boolean canAddUserToTeam(String zohoUserId, Long teamId) {
        return canManageTeam(zohoUserId, teamId);
    }

    /**
     * Check if user can remove users from team (org admin or team lead)
     */
    public boolean canRemoveUserFromTeam(String zohoUserId, Long teamId) {
        return canManageTeam(zohoUserId, teamId);
    }

    /**
     * Check if user can view team progress reports (org admin, team lead, or team member)
     */
    public boolean canViewTeamProgress(String zohoUserId, Long teamId) {
        Optional<User> userOpt = getUserByZohoId(zohoUserId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        // Org admins can view any team in their org
        if (user.getRole() == UserRole.ORG_ADMIN) {
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            return teamOpt.isPresent() 
                && teamOpt.get().getOrganization().getId().equals(user.getOrganization().getId());
        }

        // Team leads and members can view their own team
        return user.getTeam() != null && user.getTeam().getId().equals(teamId);
    }

    /**
     * Check if user can view organization-level dashboard (org admin only)
     */
    public boolean canViewOrgDashboard(String zohoUserId, Long organizationId) {
        return canManageOrganization(zohoUserId, organizationId);
    }

    /**
     * Check if user is organization admin
     */
    public boolean isOrgAdmin(String zohoUserId) {
        return getUserByZohoId(zohoUserId)
            .map(user -> user.getRole() == UserRole.ORG_ADMIN)
            .orElse(false);
    }

    /**
     * Check if user is team lead of specific team
     */
    public boolean isTeamLead(String zohoUserId, Long teamId) {
        Optional<User> userOpt = getUserByZohoId(zohoUserId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        if (user.getRole() != UserRole.TEAM_LEAD) return false;

        Optional<Team> teamOpt = teamRepository.findById(teamId);
        return teamOpt.isPresent() 
            && teamOpt.get().getTeamLeadZohoId().equals(zohoUserId);
    }

    /**
     * Check if user is team lead of any team
     */
    public boolean isTeamLead(String zohoUserId) {
        return getUserByZohoId(zohoUserId)
            .map(user -> user.getRole() == UserRole.TEAM_LEAD)
            .orElse(false);
    }

    /**
     * Check if user belongs to organization
     */
    public boolean belongsToOrganization(String zohoUserId, Long organizationId) {
        return getUserByZohoId(zohoUserId)
            .map(user -> user.getOrganization().getId().equals(organizationId))
            .orElse(false);
    }

    /**
     * Check if user belongs to team
     */
    public boolean belongsToTeam(String zohoUserId, Long teamId) {
        return getUserByZohoId(zohoUserId)
            .map(user -> user.getTeam() != null && user.getTeam().getId().equals(teamId))
            .orElse(false);
    }

    /**
     * Get user role in their organization
     */
    public Optional<UserRole> getUserRole(String zohoUserId) {
        return getUserByZohoId(zohoUserId)
            .map(User::getRole);
    }

    /**
     * Helper: Get user by Zoho ID (primary lookup method)
     */
    private Optional<User> getUserByZohoId(String zohoUserId) {
        try {
            return userRepository.findByZohoUserId(zohoUserId);
        } catch (Exception e) {
            log.error("Error finding user by Zoho ID: {}", zohoUserId, e);
            return Optional.empty();
        }
    }

    /**
     * Helper: Get user's organization
     */
    public Optional<Organization> getUserOrganization(String zohoUserId) {
        return getUserByZohoId(zohoUserId)
            .map(User::getOrganization);
    }

    /**
     * Helper: Get user's team
     */
    public Optional<Team> getUserTeam(String zohoUserId) {
        return getUserByZohoId(zohoUserId)
            .map(User::getTeam);
    }
}
