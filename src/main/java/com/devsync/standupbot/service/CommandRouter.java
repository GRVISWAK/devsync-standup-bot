package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.UserSession;
import com.devsync.standupbot.dto.ZohoUserContext;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Command router - routes incoming commands to appropriate handlers
 * Handles permission checks and multi-step conversation flows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandRouter {
    
    private final SessionManager sessionManager;
    private final PermissionService permissionService;
    private final OrganizationService organizationService;
    private final TeamService teamService;
    private final UserService userService;
    
    /**
     * Route command to appropriate handler
     */
    public String routeCommand(ZohoUserContext context) {
        String zohoUserId = context.getZohoUserId();
        String message = context.getMessage().trim().toLowerCase();
        
        // Check if user has active session (multi-step conversation)
        if (sessionManager.hasActiveSession(zohoUserId)) {
            return handleSessionContinuation(context);
        }
        
        // Route commands
        if (message.startsWith("/register-org") || message.equals("register org") || message.equals("register organization")) {
            return startOrganizationRegistration(context);
        }
        
        if (message.startsWith("/create-team") || message.equals("create team")) {
            return startTeamCreation(context);
        }
        
        if (message.startsWith("/add-user") || message.equals("add user")) {
            return startUserAddition(context);
        }
        
        if (message.startsWith("/standup") || message.equals("standup")) {
            return startStandup(context);
        }
        
        if (message.startsWith("/help") || message.equals("help")) {
            return getHelpMessage(context);
        }
        
        if (message.startsWith("/status") || message.equals("status") || message.equals("my status")) {
            return getUserStatus(context);
        }
        
        // Default response
        return "I didn't understand that command. Type **/help** to see available commands.";
    }
    
    /**
     * Handle continuation of multi-step conversation
     */
    private String handleSessionContinuation(ZohoUserContext context) {
        UserSession.SessionState state = sessionManager.getState(context.getZohoUserId());
        
        switch (state) {
            case REGISTERING_ORG:
                return handleOrganizationRegistrationFlow(context);
            case CREATING_TEAM:
                return handleTeamCreationFlow(context);
            case ADDING_USER:
                return handleUserAdditionFlow(context);
            case STANDUP_YESTERDAY:
            case STANDUP_TODAY:
            case STANDUP_BLOCKERS:
                return handleStandupFlow(context);
            case UPDATING_GITHUB:
                return handleGitHubUpdateFlow(context);
            case UPDATING_JIRA:
                return handleJiraUpdateFlow(context);
            default:
                sessionManager.resetSession(context.getZohoUserId());
                return "Session expired. Please start again.";
        }
    }
    
    /**
     * Start organization registration
     */
    private String startOrganizationRegistration(ZohoUserContext context) {
        // Check if user already registered
        if (userService.isUserRegistered(context.getZohoUserId())) {
            User user = userService.getUserByZohoId(context.getZohoUserId()).get();
            return "❌ You're already registered in organization: **" + user.getOrganization().getName() + "**\n\n" +
                   "Type **/help** to see available commands.";
        }
        
        sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.REGISTERING_ORG);
        return "🏢 **Organization Registration**\n\nWhat is your organization name?\n\n" +
               "Example: _TechCorp_, _Acme Inc_, _DevTeam_";
    }
    
    /**
     * Handle organization registration flow
     */
    private String handleOrganizationRegistrationFlow(ZohoUserContext context) {
        int step = sessionManager.getStep(context.getZohoUserId());
        String message = context.getMessage().trim();
        
        if (step == 0) {
            // Step 0: Got organization name, ask for domain
            sessionManager.putData(context.getZohoUserId(), "orgName", message);
            sessionManager.nextStep(context.getZohoUserId());
            return "Great! What is your organization's email domain?\n\n" +
                   "Example: _techcorp.com_, _acme.io_\n\n" +
                   "(Type **cancel** to abort)";
        } else if (step == 1) {
            // Step 1: Got domain, create organization
            if (message.equalsIgnoreCase("cancel")) {
                sessionManager.resetSession(context.getZohoUserId());
                return "❌ Organization registration cancelled.";
            }
            
            try {
                String orgName = sessionManager.getData(context.getZohoUserId(), "orgName", String.class);
                organizationService.registerOrganization(
                    orgName,
                    message,
                    context.getZohoUserId(),
                    context.getName(),
                    context.getEmail()
                );
                
                sessionManager.resetSession(context.getZohoUserId());
                
                return "✅ **Organization Created!**\n\n" +
                       "Organization: **" + orgName + "**\n" +
                       "Domain: **" + message + "**\n" +
                       "Your Role: **ORG_ADMIN** 👑\n\n" +
                       "You can now:\n" +
                       "• **/create-team** - Create teams\n" +
                       "• **/help** - See all commands";
            } catch (Exception e) {
                sessionManager.resetSession(context.getZohoUserId());
                return "❌ Error: " + e.getMessage();
            }
        }
        
        return "Something went wrong. Please try again with **/register-org**";
    }
    
    /**
     * Start team creation
     */
    private String startTeamCreation(ZohoUserContext context) {
        // Check if user is registered
        if (!userService.isUserRegistered(context.getZohoUserId())) {
            return "❌ Please register your organization first with **/register-org**";
        }
        
        // Check permissions
        User user = userService.getUserByZohoId(context.getZohoUserId()).get();
        if (!permissionService.canCreateTeam(context.getZohoUserId(), user.getOrganization().getId())) {
            return "❌ Only organization admins can create teams.";
        }
        
        sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.CREATING_TEAM);
        return "👥 **Team Creation**\n\nWhat is the team name?\n\n" +
               "Example: _Backend Team_, _Frontend Team_, _DevOps_";
    }
    
    /**
     * Handle team creation flow
     */
    private String handleTeamCreationFlow(ZohoUserContext context) {
        int step = sessionManager.getStep(context.getZohoUserId());
        String message = context.getMessage().trim();
        
        if (step == 0) {
            // Step 0: Got team name, ask for GitHub org
            sessionManager.putData(context.getZohoUserId(), "teamName", message);
            sessionManager.nextStep(context.getZohoUserId());
            return "What is your GitHub organization name?\n\n" +
                   "Example: _microsoft_, _google_, _facebook_\n\n" +
                   "(Type **skip** if you don't have one, or **cancel** to abort)";
        } else if (step == 1) {
            // Step 1: Got GitHub org, create team
            if (message.equalsIgnoreCase("cancel")) {
                sessionManager.resetSession(context.getZohoUserId());
                return "❌ Team creation cancelled.";
            }
            
            try {
                String teamName = sessionManager.getData(context.getZohoUserId(), "teamName", String.class);
                String githubOrg = message.equalsIgnoreCase("skip") ? null : message;
                
                Team team = teamService.createTeam(
                    context.getZohoUserId(),
                    teamName,
                    githubOrg,
                    context.getChannelId()
                );
                
                sessionManager.resetSession(context.getZohoUserId());
                
                return "✅ **Team Created!**\n\n" +
                       "Team: **" + teamName + "**\n" +
                       (githubOrg != null ? "GitHub: **" + githubOrg + "**\n" : "") +
                       "Your Role: **TEAM_LEAD** 🎖️\n\n" +
                       "Next steps:\n" +
                       "• **/add-user** - Add team members\n" +
                       "• **standup** - Submit your first standup\n" +
                       "• **/help** - See all commands";
            } catch (Exception e) {
                sessionManager.resetSession(context.getZohoUserId());
                return "❌ Error: " + e.getMessage();
            }
        }
        
        return "Something went wrong. Please try again with **/create-team**";
    }
    
    /**
     * Placeholder methods for other flows
     */
    private String startUserAddition(ZohoUserContext context) {
        return "👤 **Add User** - Coming in next implementation\n\nThis feature will allow you to add team members with their GitHub and Jira credentials.";
    }
    
    private String handleUserAdditionFlow(ZohoUserContext context) {
        return "User addition flow - to be implemented";
    }
    
    private String startStandup(ZohoUserContext context) {
        return "📝 **Standup** - Coming in next implementation\n\nThis will auto-fetch your GitHub commits and Jira issues for quick standup submission.";
    }
    
    private String handleStandupFlow(ZohoUserContext context) {
        return "Standup flow - to be implemented";
    }
    
    private String handleGitHubUpdateFlow(ZohoUserContext context) {
        return "GitHub update flow - to be implemented";
    }
    
    private String handleJiraUpdateFlow(ZohoUserContext context) {
        return "Jira update flow - to be implemented";
    }
    
    /**
     * Get help message based on user's role
     */
    private String getHelpMessage(ZohoUserContext context) {
        if (!userService.isUserRegistered(context.getZohoUserId())) {
            return "**DevSync Standup Bot** 🤖\n\n" +
                   "**Getting Started:**\n" +
                   "• **/register-org** - Register your organization\n\n" +
                   "Once registered, you can create teams, add users, and start daily standups!";
        }
        
        User user = userService.getUserByZohoId(context.getZohoUserId()).get();
        StringBuilder help = new StringBuilder("**DevSync Standup Bot** 🤖\n\n");
        help.append("Organization: **").append(user.getOrganization().getName()).append("**\n");
        help.append("Your Role: **").append(user.getRole()).append("**\n\n");
        
        help.append("**Available Commands:**\n");
        
        if (permissionService.isOrgAdmin(context.getZohoUserId())) {
            help.append("• **/create-team** - Create new team\n");
        }
        
        if (user.getTeam() != null) {
            if (permissionService.isTeamLead(context.getZohoUserId())) {
                help.append("• **/add-user** - Add team member\n");
            }
            help.append("• **standup** - Submit daily standup\n");
            help.append("• **/status** - View your profile\n");
        }
        
        help.append("• **/help** - Show this message\n");
        
        return help.toString();
    }
    
    /**
     * Get user status/profile
     */
    private String getUserStatus(ZohoUserContext context) {
        if (!userService.isUserRegistered(context.getZohoUserId())) {
            return "❌ You're not registered. Type **/register-org** to get started.";
        }
        
        User user = userService.getUserByZohoId(context.getZohoUserId()).get();
        StringBuilder status = new StringBuilder("**Your Profile** 👤\n\n");
        status.append("Name: **").append(user.getName()).append("**\n");
        status.append("Email: **").append(user.getEmail()).append("**\n");
        status.append("Organization: **").append(user.getOrganization().getName()).append("**\n");
        status.append("Role: **").append(user.getRole()).append("**\n");
        
        if (user.getTeam() != null) {
            status.append("Team: **").append(user.getTeam().getTeamName()).append("**\n");
        }
        
        if (user.getGithubUsername() != null) {
            status.append("GitHub: **").append(user.getGithubUsername()).append("** ✅\n");
        } else {
            status.append("GitHub: ❌ _Not configured_\n");
        }
        
        if (user.getJiraEmail() != null) {
            status.append("Jira: **").append(user.getJiraEmail()).append("** ✅\n");
        } else {
            status.append("Jira: ❌ _Not configured_\n");
        }
        
        return status.toString();
    }
}
