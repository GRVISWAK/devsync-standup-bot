package com.devsync.standupbot.service;

import com.devsync.standupbot.dto.UserSession;
import com.devsync.standupbot.dto.ZohoUserContext;
import com.devsync.standupbot.model.Standup;
import com.devsync.standupbot.model.Team;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.repository.StandupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final GitHubService githubService;
    private final JiraService jiraService;
    private final AIService aiService;
    private final StandupRepository standupRepository;
    
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
     * Start user addition
     */
    private String startUserAddition(ZohoUserContext context) {
        // Check if user is registered and has team
        if (!userService.isUserRegistered(context.getZohoUserId())) {
            return "❌ Please register your organization first with **/register-org**";
        }
        
        User user = userService.getUserByZohoId(context.getZohoUserId()).get();
        if (user.getTeam() == null) {
            return "❌ You must be part of a team to add users. Create a team with **/create-team** first.";
        }
        
        // Check permissions
        if (!permissionService.canAddUserToTeam(context.getZohoUserId(), user.getTeam().getId())) {
            return "❌ Only team leads and organization admins can add users.";
        }
        
        sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.ADDING_USER);
        sessionManager.putData(context.getZohoUserId(), "teamId", user.getTeam().getId());
        
        return "👤 **Add User to Team**\n\n" +
               "Please mention the user you want to add.\n\n" +
               "Example: _@JohnDoe_\n\n" +
               "(Type **cancel** to abort)";
    }
    
    /**
     * Handle user addition flow
     */
    private String handleUserAdditionFlow(ZohoUserContext context) {
        int step = sessionManager.getStep(context.getZohoUserId());
        String message = context.getMessage().trim();
        
        if (message.equalsIgnoreCase("cancel")) {
            sessionManager.resetSession(context.getZohoUserId());
            return "❌ User addition cancelled.";
        }
        
        if (step == 0) {
            // Step 0: Got mentioned user - extract Zoho ID, name, email
            // For now, ask for email manually (in real Zoho integration, we'd extract from mention)
            sessionManager.putData(context.getZohoUserId(), "newUserName", message.replace("@", ""));
            sessionManager.nextStep(context.getZohoUserId());
            return "What is " + message + "'s email address?";
        } else if (step == 1) {
            // Step 1: Got email, ask for GitHub username
            sessionManager.putData(context.getZohoUserId(), "newUserEmail", message);
            sessionManager.nextStep(context.getZohoUserId());
            return "What is their GitHub username?\n\n(Type **skip** if they don't have one)";
        } else if (step == 2) {
            // Step 2: Got GitHub username, ask for GitHub token
            if (!message.equalsIgnoreCase("skip")) {
                sessionManager.putData(context.getZohoUserId(), "githubUsername", message);
                sessionManager.nextStep(context.getZohoUserId());
                return "What is their GitHub Personal Access Token?\n\n" +
                       "_This is needed to auto-fetch their commits during standup._\n\n" +
                       "(Type **skip** to configure later)";
            } else {
                // Skip GitHub token - manually advance 2 steps (skip token + jira email questions)
                sessionManager.getSession(context.getZohoUserId()).setStep(4);
                return "What is their Jira email?\n\n(Type **skip** if they don't use Jira)";
            }
        } else if (step == 3) {
            // Step 3: Got GitHub token, ask for Jira email
            if (!message.equalsIgnoreCase("skip")) {
                sessionManager.putData(context.getZohoUserId(), "githubToken", message);
            }
            sessionManager.nextStep(context.getZohoUserId());
            return "What is their Jira email?\n\n(Type **skip** if they don't use Jira)";
        } else if (step == 4) {
            // Step 4: Got Jira email, ask for Jira account ID
            if (!message.equalsIgnoreCase("skip")) {
                sessionManager.putData(context.getZohoUserId(), "jiraEmail", message);
                sessionManager.nextStep(context.getZohoUserId());
                return "What is their Jira Account ID?\n\n(Type **skip** to configure later)";
            } else {
                // No Jira, create user
                return createUserFromSession(context);
            }
        } else if (step == 5) {
            // Step 5: Got Jira account ID, ask for Jira API token
            if (!message.equalsIgnoreCase("skip")) {
                sessionManager.putData(context.getZohoUserId(), "jiraAccountId", message);
                sessionManager.nextStep(context.getZohoUserId());
                return "What is their Jira API Token?\n\n(Type **skip** to configure later)";
            } else {
                return createUserFromSession(context);
            }
        } else if (step == 6) {
            // Step 6: Got Jira API token, create user
            if (!message.equalsIgnoreCase("skip")) {
                sessionManager.putData(context.getZohoUserId(), "jiraApiToken", message);
            }
            return createUserFromSession(context);
        }
        
        return "Something went wrong. Please try again with **/add-user**";
    }
    
    /**
     * Create user from session data
     */
    private String createUserFromSession(ZohoUserContext context) {
        try {
            Long teamId = sessionManager.getData(context.getZohoUserId(), "teamId", Long.class);
            Team team = teamService.getTeamById(teamId).orElseThrow();
            
            String newUserName = sessionManager.getData(context.getZohoUserId(), "newUserName", String.class);
            String newUserEmail = sessionManager.getData(context.getZohoUserId(), "newUserEmail", String.class);
            
            // Generate temporary Zoho ID (in real integration, this comes from @mention)
            String newUserZohoId = "temp_" + System.currentTimeMillis();
            
            // Register user
            User newUser = userService.registerUser(
                context.getZohoUserId(),
                team,
                newUserZohoId,
                newUserName,
                newUserEmail
            );
            
            // Update GitHub credentials if provided
            String githubUsername = sessionManager.getData(context.getZohoUserId(), "githubUsername", String.class);
            String githubToken = sessionManager.getData(context.getZohoUserId(), "githubToken", String.class);
            if (githubUsername != null && githubToken != null) {
                userService.updateGitHubCredentials(newUserZohoId, githubUsername, githubToken);
            }
            
            // Update Jira credentials if provided
            String jiraAccountId = sessionManager.getData(context.getZohoUserId(), "jiraAccountId", String.class);
            String jiraEmail = sessionManager.getData(context.getZohoUserId(), "jiraEmail", String.class);
            String jiraApiToken = sessionManager.getData(context.getZohoUserId(), "jiraApiToken", String.class);
            if (jiraAccountId != null && jiraEmail != null && jiraApiToken != null) {
                userService.updateJiraCredentials(newUserZohoId, jiraAccountId, jiraEmail, jiraApiToken);
            }
            
            sessionManager.resetSession(context.getZohoUserId());
            
            return "✅ **User Added Successfully!**\n\n" +
                   "Name: **" + newUserName + "**\n" +
                   "Email: **" + newUserEmail + "**\n" +
                   "Team: **" + team.getTeamName() + "**\n" +
                   "Role: **DEVELOPER**\n" +
                   (githubUsername != null ? "GitHub: **" + githubUsername + "** ✅\n" : "") +
                   (jiraEmail != null ? "Jira: **" + jiraEmail + "** ✅\n" : "") +
                   "\nThey can now submit standups with **standup** command!";
            
        } catch (Exception e) {
            sessionManager.resetSession(context.getZohoUserId());
            return "❌ Error adding user: " + e.getMessage();
        }
    }
    
    /**
     * Start standup
     */
    private String startStandup(ZohoUserContext context) {
        // Check if user is registered
        if (!userService.isUserRegistered(context.getZohoUserId())) {
            return "❌ Please register your organization first with **/register-org**";
        }
        
        User user = userService.getUserByZohoId(context.getZohoUserId()).get();
        
        if (user.getTeam() == null) {
            return "❌ You must join a team before submitting standups.";
        }
        
        // Check if standup already submitted today
        LocalDate today = LocalDate.now();
        if (standupRepository.existsByUserAndStandupDate(user, today)) {
            return "✅ You've already submitted standup for today!\n\nType **/status** to view your profile.";
        }
        
        // Fetch GitHub commits and Jira issues
        StringBuilder context_info = new StringBuilder();
        
        if (user.getGithubUsername() != null && user.getGithubToken() != null) {
            try {
                List<String> commits = githubService.fetchRecentCommits(
                    user.getGithubUsername(),
                    user.getGithubToken()
                );
                
                if (!commits.isEmpty()) {
                    context_info.append("\n**📝 Your GitHub Commits (Last 24h):**\n");
                    for (String commit : commits) {
                        context_info.append(commit).append("\n");
                    }
                    sessionManager.putData(context.getZohoUserId(), "githubCommits", commits);
                }
            } catch (Exception e) {
                log.error("Error fetching GitHub commits", e);
            }
        }
        
        if (user.getJiraAccountId() != null && user.getJiraApiToken() != null && user.getTeam() != null) {
            try {
                Team team = user.getTeam();
                if (team.getJiraApiUrl() != null) {
                    List<String> issues = jiraService.fetchActiveTasks(
                        user.getJiraAccountId(),
                        team.getJiraApiUrl(),
                        user.getJiraEmail(),
                        user.getJiraApiToken()
                    );
                    
                    if (!issues.isEmpty()) {
                        context_info.append("\n**🎫 Your Jira Issues (Updated Last 24h):**\n");
                        for (String issue : issues) {
                            context_info.append(issue).append("\n");
                        }
                        sessionManager.putData(context.getZohoUserId(), "jiraIssues", issues);
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching Jira issues", e);
            }
        }
        
        sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.STANDUP_YESTERDAY);
        
        return "📝 **Daily Standup**\n" +
               context_info.toString() +
               "\n**What did you accomplish yesterday?**\n\n" +
               "_Describe your work, or type **auto** to use the commits/issues above._";
    }
    
    /**
     * Handle standup flow
     */
    private String handleStandupFlow(ZohoUserContext context) {
        UserSession.SessionState state = sessionManager.getState(context.getZohoUserId());
        String message = context.getMessage().trim();
        
        if (message.equalsIgnoreCase("cancel")) {
            sessionManager.resetSession(context.getZohoUserId());
            return "❌ Standup cancelled.";
        }
        
        if (state == UserSession.SessionState.STANDUP_YESTERDAY) {
            // Got yesterday's work
            sessionManager.putData(context.getZohoUserId(), "yesterdayWork", message);
            sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.STANDUP_TODAY);
            return "**What are you planning to do today?**";
        } else if (state == UserSession.SessionState.STANDUP_TODAY) {
            // Got today's plan
            sessionManager.putData(context.getZohoUserId(), "todayPlan", message);
            sessionManager.setState(context.getZohoUserId(), UserSession.SessionState.STANDUP_BLOCKERS);
            return "**Any blockers or challenges?**\n\n(Type **none** if no blockers)";
        } else if (state == UserSession.SessionState.STANDUP_BLOCKERS) {
            // Got blockers, create standup
            return createStandupFromSession(context, message);
        }
        
        return "Something went wrong. Please try again with **standup**";
    }
    
    /**
     * Create standup from session data
     */
    private String createStandupFromSession(ZohoUserContext context, String blockers) {
        try {
            User user = userService.getUserByZohoId(context.getZohoUserId()).get();
            
            String yesterdayWork = sessionManager.getData(context.getZohoUserId(), "yesterdayWork", String.class);
            String todayPlan = sessionManager.getData(context.getZohoUserId(), "todayPlan", String.class);
            
            // Get GitHub commits and Jira issues from session
            @SuppressWarnings("unchecked")
            List<String> githubCommits = (List<String>) sessionManager.getData(context.getZohoUserId(), "githubCommits");
            @SuppressWarnings("unchecked")
            List<String> jiraIssues = (List<String>) sessionManager.getData(context.getZohoUserId(), "jiraIssues");
            
            if (githubCommits == null) githubCommits = new ArrayList<>();
            if (jiraIssues == null) jiraIssues = new ArrayList<>();
            
            // Generate AI summary with GitHub and Jira context
            String aiSummary = aiService.generateStandupSummary(yesterdayWork, todayPlan, blockers,
                githubCommits, jiraIssues, new ArrayList<>());
            
            // Create standup
            Standup standup = Standup.builder()
                .user(user)
                .standupDate(LocalDate.now())
                .yesterdayWork(yesterdayWork)
                .todayPlan(todayPlan)
                .blockers(blockers.equalsIgnoreCase("none") ? null : blockers)
                .status(Standup.StandupStatus.COMPLETED)
                .aiSummary(aiSummary)
                .build();
            
            standupRepository.save(standup);
            sessionManager.resetSession(context.getZohoUserId());
            
            return "✅ **Standup Submitted!**\n\n" +
                   "**AI Summary:**\n" + aiSummary + "\n\n" +
                   "Great work! 🎉";
            
        } catch (Exception e) {
            log.error("Error creating standup", e);
            sessionManager.resetSession(context.getZohoUserId());
            return "❌ Error submitting standup: " + e.getMessage();
        }
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
