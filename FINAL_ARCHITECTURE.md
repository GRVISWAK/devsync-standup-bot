# 🏗️ DevSync StandupBot - Final Architecture

## 📋 **Core Requirements Summary**

### **Hierarchy**
```
Organization
  ├── Teams
  │   ├── Team Lead
  │   └── Members
  └── Integrations (GitHub, Jira, AI)
```

### **User Roles**
- **Org Admin**: Full organization control
- **Team Lead**: Team management + reporting
- **Developer**: Self-service standup

---

## 🎯 **Feature Breakdown**

### **Phase 1: Foundation (CRITICAL - Implement First)**

#### 1.1 Organization Management
```
Commands:
- create org name:TechCorp domain:techcorp.com
- org info
- list orgs

Who: First user becomes Org Admin
DB: organizations table
Fields: id, name, domain, created_by_zoho_id, active, created_at
```

#### 1.2 Team Management
```
Commands:
- create team name:Engineering orgid:1
- team info
- list teams

Who: Org members create teams (creator = Team Lead)
DB: teams table
Fields: id, org_id, name, team_lead_zoho_id, github_org, created_at
```

#### 1.3 User Management (by Team Lead)
```
Commands:
- add user @mention github:user jira:user@atlassian
- remove user @mention
- update user @mention github:newuser
- transfer user @mention teamid:2
- list members

Who: Team Lead only
DB: users table
Fields: id, zoho_user_id, name, email, team_id, role, 
        github_username, jira_email, jira_api_token, created_at
```

#### 1.4 Auto User Detection
```
Webhook receives:
{
  "user": {
    "id": "zoho_12345",
    "name": "John Doe", 
    "email": "john@company.com"
  },
  "message": "standup"
}

Bot looks up: zoho_user_id → Gets user profile → Starts standup
No need to type email!
```

---

### **Phase 2: Standup Workflows (CORE FUNCTIONALITY)**

#### 2.1 Individual Standup
```
Command: standup (no parameters!)

Flow:
1. Bot: "Hi John! What did you work on yesterday?"
   User: "Fixed authentication bug"

2. Bot: "Great! What will you work on today?"
   User: "Build user dashboard"

3. Bot: "Any blockers?"
   User: "none"

4. Bot fetches:
   - GitHub commits (last 24h)
   - Jira issues (updated today)
   - Generates AI summary

5. Bot displays:
   ✅ Standup Complete!
   
   📝 Yesterday: Fixed authentication bug
   🎯 Today: Build user dashboard  
   🚧 Blockers: None
   
   📊 GitHub: 5 commits in auth-service
   🎫 Jira: PROJ-123 (In Progress)
   
   🤖 AI Summary: John made critical security fixes...
```

#### 2.2 Team Lead: Team Standup Summary
```
Command: team standup

Who: Team Lead only

Output:
📊 Engineering Team Standup - Nov 24, 2025

✅ Completed (3/5):
• John - Dashboard work, no blockers
• Alice - Code review, blocked by PR approval
• Bob - Testing, no blockers

⏳ Pending (2/5):
• Charlie - Not submitted
• David - Not submitted

🚧 Team Blockers:
• Alice: Waiting for PR approval (#234)

📈 Team Activity:
• GitHub: 15 total commits
• Jira: 8 issues in progress

🤖 AI Team Summary:
Team is making good progress on dashboard feature.
Alice needs help with PR review. Charlie and David
need reminders.
```

#### 2.3 Individual Progress Check
```
Command: progress @username

Who: Team Lead or user checking own progress

Output:
📊 Progress Report: @John
Period: Last 7 days

📝 Standups: 5/5 completed (100%)

📊 GitHub Activity:
• 23 commits across 3 repos
• Top: auth-service (15 commits)
• Languages: Java (80%), YAML (20%)

🎫 Jira Activity:
• 3 issues completed
• 2 issues in progress
• 0 blockers

📅 Attendance:
• ✅ No missed standups
• Average submit time: 9:15 AM

🤖 AI Insights:
Consistent contributor with focus on backend security.
Velocity stable. No concerning patterns.
```

---

### **Phase 3: Advanced Features (ENHANCE EXPERIENCE)**

#### 3.1 Organization Dashboard
```
Command: org summary

Who: Org Admin only

Output:
📊 TechCorp Organization Dashboard

Teams: 3
Total Members: 25

📈 Standup Completion:
• Engineering: 8/10 (80%)
• Marketing: 5/7 (71%)
• Sales: 6/8 (75%)

🏆 Top Contributors:
1. John - 100% attendance
2. Alice - 98% attendance
3. Bob - 95% attendance

⚠️ Needs Attention:
• Charlie - 3 missed standups this week
• David - 2 days late submissions
```

#### 3.2 Attendance Tracking
```
DB: standup_attendance table
Fields: user_id, date, submitted_at, status (ON_TIME, LATE, MISSED)

Auto-reminders:
- 9:00 AM: "Time for your standup!"
- 11:00 AM: "You haven't submitted standup yet"
- 5:00 PM: "You missed today's standup"

Team Lead notifications:
- Daily summary of who missed standup
```

#### 3.3 Mentions Support
```
When user types: progress @alice

Zoho sends:
{
  "mentions": [
    {"id": "zoho_67890", "name": "Alice"}
  ],
  "message": "progress @alice"
}

Bot extracts: mentioned_user_id → DB lookup → Show Alice's progress
```

---

## 📊 **Database Schema**

### **1. Organizations**
```sql
CREATE TABLE organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(100),
    created_by_zoho_id VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### **2. Teams**
```sql
CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    team_lead_zoho_id VARCHAR(100) NOT NULL,
    zoho_channel_id VARCHAR(100),
    github_org VARCHAR(100),
    github_token VARCHAR(500),  -- Team-level token
    jira_api_url VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_team_per_org (organization_id, team_name),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_team_lead (team_lead_zoho_id)
);
```

### **3. Users**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    zoho_user_id VARCHAR(100) NOT NULL UNIQUE,  -- PRIMARY LOOKUP KEY
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    team_id BIGINT,
    organization_id BIGINT,
    role ENUM('ORG_ADMIN', 'TEAM_LEAD', 'DEVELOPER') DEFAULT 'DEVELOPER',
    github_username VARCHAR(100),
    jira_email VARCHAR(255),
    jira_api_token VARCHAR(500),  -- User-level override
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_zoho_user (zoho_user_id),
    INDEX idx_team (team_id)
);
```

### **4. Standups**
```sql
CREATE TABLE standups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    zoho_user_id VARCHAR(100) NOT NULL,  -- For quick lookup
    team_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    standup_date DATE NOT NULL,
    yesterday_work TEXT,
    today_plan TEXT,
    blockers TEXT,
    github_commits TEXT,  -- JSON array
    jira_tasks TEXT,      -- JSON array
    calendar_events TEXT, -- JSON array
    ai_summary TEXT,
    status ENUM('IN_PROGRESS', 'COMPLETED') DEFAULT 'IN_PROGRESS',
    current_step INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    UNIQUE KEY unique_standup_per_day (user_id, standup_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    INDEX idx_date (standup_date),
    INDEX idx_user_date (user_id, standup_date)
);
```

### **5. Standup Attendance**
```sql
CREATE TABLE standup_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('ON_TIME', 'LATE', 'MISSED') DEFAULT 'MISSED',
    submitted_at TIMESTAMP,
    reminded_at TIMESTAMP,
    UNIQUE KEY unique_attendance (user_id, attendance_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_date (attendance_date)
);
```

### **6. User Sessions (for multi-step conversations)**
```sql
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    zoho_user_id VARCHAR(100) NOT NULL,
    session_type ENUM('STANDUP', 'ADD_USER', 'CREATE_TEAM') NOT NULL,
    session_data TEXT,  -- JSON data
    current_step INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE KEY unique_active_session (zoho_user_id, session_type)
);
```

---

## 🔧 **Technical Architecture**

### **1. Webhook Controller Enhancement**
```java
@PostMapping("/webhook")
public ResponseEntity<Map<String, Object>> handleWebhook(
    @RequestBody Map<String, Object> payload) {
    
    // Extract Zoho user info
    Map<String, Object> userInfo = (Map<String, Object>) payload.get("user");
    String zohoUserId = (String) userInfo.get("id");
    String userName = (String) userInfo.get("name");
    String userEmail = (String) userInfo.get("email");
    
    // Extract message and mentions
    String message = (String) payload.get("text");
    List<Map<String, Object>> mentions = 
        (List<Map<String, Object>>) payload.get("mentions");
    
    // Route to command handler with full context
    return commandRouter.route(
        new CommandContext(zohoUserId, userName, userEmail, message, mentions)
    );
}
```

### **2. Command Router Pattern**
```java
public class CommandRouter {
    
    private Map<String, CommandHandler> handlers;
    
    public ResponseEntity<Map<String, Object>> route(CommandContext ctx) {
        String command = extractCommand(ctx.getMessage());
        
        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            return handleUnknownCommand(ctx);
        }
        
        // Permission check
        if (!handler.hasPermission(ctx)) {
            return permissionDenied(ctx);
        }
        
        return handler.execute(ctx);
    }
}
```

### **3. Permission Service**
```java
@Service
public class PermissionService {
    
    public boolean canManageTeam(String zohoUserId, Long teamId) {
        User user = userRepository.findByZohoUserId(zohoUserId);
        if (user.getRole() == UserRole.ORG_ADMIN) return true;
        
        Team team = teamRepository.findById(teamId);
        return team.getTeamLeadZohoId().equals(zohoUserId);
    }
    
    public boolean canViewProgress(String requesterZohoId, String targetZohoId) {
        if (requesterZohoId.equals(targetZohoId)) return true; // Self
        
        User requester = userRepository.findByZohoUserId(requesterZohoId);
        User target = userRepository.findByZohoUserId(targetZohoId);
        
        // Same team + requester is team lead
        if (requester.getTeamId().equals(target.getTeamId())) {
            return requester.getRole() == UserRole.TEAM_LEAD ||
                   requester.getRole() == UserRole.ORG_ADMIN;
        }
        
        // Same org + requester is org admin
        return requester.getOrganizationId().equals(target.getOrganizationId()) &&
               requester.getRole() == UserRole.ORG_ADMIN;
    }
}
```

### **4. Integration Services**

#### GitHub Service
```java
@Service
public class GitHubService {
    
    public List<CommitInfo> fetchCommits(String username, String token, int hours) {
        // Use user's GitHub username from DB
        // Fetch commits from last N hours
        // Handle rate limits
        // Return structured commit data
    }
    
    public TeamGitHubSummary fetchTeamCommits(Team team, int hours) {
        // Fetch for all team members
        // Aggregate by repo
        // Return team summary
    }
}
```

#### Jira Service
```java
@Service
public class JiraService {
    
    public List<JiraIssue> fetchUserIssues(String jiraEmail, String token) {
        // Fetch issues assigned to user
        // Filter: In Progress, Updated Today
        // Include blockers
    }
    
    public TeamJiraSummary fetchTeamIssues(Team team) {
        // Fetch for all team members
        // Group by status
        // Identify blockers
    }
}
```

#### AI Service (Enhanced)
```java
@Service
public class AIService {
    
    public String generateStandupSummary(Standup standup, 
                                         List<CommitInfo> commits,
                                         List<JiraIssue> issues) {
        String prompt = buildPrompt(standup, commits, issues);
        return geminiClient.generate(prompt);
    }
    
    public String generateTeamSummary(List<Standup> standups,
                                      TeamGitHubSummary github,
                                      TeamJiraSummary jira) {
        String prompt = buildTeamPrompt(standups, github, jira);
        return geminiClient.generate(prompt);
    }
    
    public ProgressInsights analyzeUserProgress(User user, 
                                                List<Standup> standups,
                                                int days) {
        // Detect patterns: consistent blockers, low activity, etc.
        // Flag anomalies
        // Suggest improvements
    }
}
```

---

## 📱 **Command Reference (Complete)**

### **Organization Commands**
```
create org name:TechCorp domain:techcorp.com
org info
list orgs
```

### **Team Commands**
```
create team name:Engineering orgid:1
team info
list teams
team standup                    # Team Lead only
```

### **User Management (Team Lead)**
```
add user @mention github:user jira:user@atlassian
remove user @mention
update user @mention github:newuser jira:new@atlassian
transfer user @mention teamid:2
list members
```

### **Standup Commands**
```
standup                         # Auto-detect user
my standups                     # View history
team standups                   # Team Lead: view team
```

### **Progress & Reports**
```
progress @mention               # User progress
team progress                   # Team Lead: all members
org summary                     # Org Admin only
```

### **Profile Commands**
```
my profile
update profile github:newuser jira:new@atlassian
```

---

## 🚀 **Implementation Phases**

### **Phase 1: Foundation (Week 1)**
- ✅ Organization entity + CRUD
- ✅ Team entity + hierarchy
- ✅ User entity + Zoho ID lookup
- ✅ Permission system
- ✅ Command router
- ✅ Basic webhook parsing

### **Phase 2: Core Standup (Week 2)**
- ✅ Individual standup workflow
- ✅ GitHub integration
- ✅ Jira integration
- ✅ AI summary generation
- ✅ Session management

### **Phase 3: Team Features (Week 3)**
- ✅ Team standup summary
- ✅ Progress reports
- ✅ Attendance tracking
- ✅ Auto-reminders

### **Phase 4: Advanced (Week 4)**
- ✅ Org dashboard
- ✅ Analytics & insights
- ✅ Transfer users
- ✅ Bulk operations

---

## 🎯 **Key Design Decisions**

1. **Primary Lookup: Zoho User ID**
   - Fast, unique, never changes
   - Email can change, Zoho ID can't

2. **Permissions: Role-based**
   - Org Admin > Team Lead > Developer
   - Clear hierarchy

3. **Integrations: Token Hierarchy**
   - Team-level GitHub/Jira tokens (default)
   - User can override with personal tokens

4. **Sessions: Stateful Conversations**
   - Store in DB (for scale across instances)
   - Auto-expire after 1 hour

5. **AI: Context-aware**
   - Include GitHub commits in prompt
   - Include Jira status in prompt
   - Detect patterns and anomalies

---

## ✅ **Success Metrics**

- **Adoption**: 80%+ daily standup completion
- **Speed**: < 2s response time
- **Accuracy**: GitHub/Jira data 100% accurate
- **Engagement**: AI summaries rated useful by 90%+ users

---

## 📝 **Next Steps**

Ready to implement? I'll start with:

1. Create Organization model
2. Update Team model
3. Update User model  
4. Create Permission service
5. Enhance webhook controller
6. Add command router
7. Test with Zoho webhook

Estimated: 45-60 minutes for Phase 1.

**Should I proceed?** 🚀
