# 🎯 StandupBot Redesign Proposal

## Problem Statement

Current issues:
1. ❌ No organization concept
2. ❌ Team names get corrupted (e.g., `'engineering"}'`)
3. ❌ Anyone can create/join any team
4. ❌ No unique user identification (using email instead of Zoho User ID)
5. ❌ No team ownership/permissions

---

## 🏗️ New Architecture

### **Hierarchy:**
```
Organization → Team → User → Standup
```

### **User Flow:**

#### **Step 1: Organization Setup (One-time)**
```
Admin: "create org name:TechCorp domain:techcorp.com"
→ Bot: "✅ Organization created! Org ID: 1"
```

#### **Step 2: Team Lead Creates Team**
```
TeamLead: "create team name:Engineering orgid:1"
→ Bot: "✅ Team created! You are the team lead. Team ID: 1"
```

#### **Step 3: Team Lead Adds Members**
```
TeamLead: "add user @JohnDoe teamid:1 github:johndoe jira:john.doe@atlassian"
→ Bot: "@JohnDoe has been added to Engineering team!"
```

**OR Self-Registration:**
```
Developer: "join team teamid:1"
→ Bot: "Join request sent to team lead for approval"

TeamLead: "approve user @JohnDoe"
→ Bot: "@JohnDoe approved! Welcome to Engineering team."
```

#### **Step 4: Daily Standup (Auto-detect user)**
```
Developer: "standup"
→ Bot: "Hi John! What did you work on yesterday?"

Developer: "Fixed authentication bug"
→ Bot: "Great! What will you work on today?"

Developer: "Implement user dashboard"
→ Bot: "Any blockers?"

Developer: "none"
→ Bot: "✅ Standup complete! [AI Summary + GitHub commits + Jira tasks]"
```

---

## 📋 New Database Schema

### **1. Organizations Table**
```sql
CREATE TABLE organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_name VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(100),
    created_by_zoho_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);
```

### **2. Teams Table (Updated)**
```sql
CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    team_lead_zoho_id VARCHAR(100) NOT NULL,
    zoho_channel_id VARCHAR(100),
    github_org VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY unique_team_per_org (organization_id, team_name),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

### **3. Users Table (Updated)**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    zoho_user_id VARCHAR(100) NOT NULL UNIQUE,  -- PRIMARY IDENTIFIER
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    team_id BIGINT,
    github_username VARCHAR(100),
    jira_account_id VARCHAR(100),
    role ENUM('TEAM_LEAD', 'DEVELOPER', 'ADMIN') DEFAULT 'DEVELOPER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id)
);
```

### **4. Standups Table (Updated)**
```sql
CREATE TABLE standups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    zoho_user_id VARCHAR(100) NOT NULL,  -- For quick lookup
    standup_date DATE NOT NULL,
    yesterday_work TEXT,
    today_plan TEXT,
    blockers TEXT,
    ai_summary TEXT,
    github_commits TEXT,
    jira_tasks TEXT,
    status ENUM('IN_PROGRESS', 'COMPLETED') DEFAULT 'IN_PROGRESS',
    submitted_at TIMESTAMP,
    UNIQUE KEY unique_standup_per_day (user_id, standup_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🎯 Command Reference

### **Organization Management**
| Command | Who Can Use | Example |
|---------|-------------|---------|
| `create org name:X domain:Y` | Anyone (first user becomes admin) | `create org name:TechCorp domain:techcorp.com` |
| `org info` | Organization members | `org info` |

### **Team Management**
| Command | Who Can Use | Example |
|---------|-------------|---------|
| `create team name:X orgid:Y` | Organization members | `create team name:Engineering orgid:1` |
| `add user @mention teamid:X` | Team leads only | `add user @JohnDoe teamid:1 github:johndoe` |
| `remove user @mention` | Team leads only | `remove user @JohnDoe` |
| `team info` | Team members | `team info` |

### **User Management**
| Command | Who Can Use | Example |
|---------|-------------|---------|
| `join team teamid:X` | Any Zoho user | `join team teamid:1` |
| `approve user @mention` | Team leads only | `approve @JohnDoe` |
| `my profile` | Any registered user | `my profile` |
| `update profile github:X jira:Y` | Any registered user | `update profile github:johndoe` |

### **Standup (Auto-detect user from Zoho)**
| Command | Who Can Use | Example |
|---------|-------------|---------|
| `standup` | Registered team members | `standup` |
| `yesterday: X` | During standup session | `yesterday: Fixed bug` |
| `today: X` | During standup session | `today: Build feature` |
| `blockers: X` | During standup session | `blockers: none` |
| `my standups` | Any registered user | `my standups` |
| `team standups` | Team members | `team standups` |

---

## 🔧 Implementation Changes

### **1. Parse Zoho Webhook Properly**

```java
@PostMapping("/webhook")
public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> payload) {
    // Extract Zoho user info
    Map<String, Object> userInfo = (Map<String, Object>) payload.get("user");
    String zohoUserId = (String) userInfo.get("id");
    String userName = (String) userInfo.get("name");
    String userEmail = (String) userInfo.get("email");
    
    String message = (String) payload.get("message");
    
    // Now we KNOW who is making the request!
    return handleCommand(zohoUserId, userName, userEmail, message);
}
```

### **2. Auto-detect User for Standup**

```java
private Map<String, Object> handleStandup(String zohoUserId) {
    // No need to ask for email!
    User user = userRepository.findByZohoUserId(zohoUserId)
        .orElseThrow(() -> new UserNotFoundException("Please join a team first!"));
    
    // Start standup automatically
    Standup standup = createOrGetTodayStandup(user);
    return askNextQuestion(standup);
}
```

### **3. Permission Checks**

```java
private boolean isTeamLead(String zohoUserId, Long teamId) {
    Team team = teamRepository.findById(teamId).orElseThrow();
    return team.getTeamLeadZohoId().equals(zohoUserId);
}

private void validateTeamLead(String zohoUserId, Long teamId) {
    if (!isTeamLead(zohoUserId, teamId)) {
        throw new PermissionDeniedException("Only team lead can perform this action!");
    }
}
```

---

## 🎨 User Experience Examples

### **Example 1: New Organization Setup**
```
Alice: "create org name:TechCorp domain:techcorp.com"
Bot: "✅ Organization 'TechCorp' created!
      • Org ID: 1
      • Admin: Alice
      • Domain: techcorp.com
      
      Next: Create teams with 'create team name:Engineering orgid:1'"

Alice: "create team name:Engineering orgid:1"
Bot: "✅ Team 'Engineering' created!
      • Team ID: 1
      • Team Lead: Alice
      • Organization: TechCorp
      
      Add members: 'add user @mention teamid:1'"

Alice: "add user @Bob teamid:1 github:bobdev jira:bob@atlassian"
Bot: "@Bob has been added to Engineering team!
      @Bob can now submit standups with 'standup' command"
```

### **Example 2: Daily Standup (No email needed!)**
```
Bob: "standup"
Bot: "Hi Bob! 👋 What did you work on yesterday?"

Bob: "Fixed login authentication bug"
Bot: "Great! What will you work on today?"

Bob: "Implement user dashboard"
Bot: "Any blockers or challenges?"

Bob: "none"
Bot: "🎉 Standup Complete!

      📊 Summary:
      Yesterday: Fixed login authentication bug
      Today: Implement user dashboard
      Blockers: None
      
      🤖 AI Insight: Bob made progress on critical authentication 
      security. Focus shifting to user experience improvements.
      
      📝 GitHub: 3 commits (auth-service, user-module)
      🎯 Jira: PROJ-123 (In Progress)"
```

### **Example 3: Team Stats**
```
Alice: "team standups"
Bot: "📊 Engineering Team - Daily Standups
      Date: Nov 24, 2025
      
      ✅ Completed (2/3):
      • Bob - Dashboard development
      • Alice - Code review
      
      ⏳ Pending (1/3):
      • Charlie - Not submitted
      
      Type 'remind @Charlie' to send reminder"
```

---

## ✅ Benefits of New Design

1. **Proper Organization Hierarchy**
   - Multiple teams under one organization
   - Clear ownership and permissions

2. **Better User Experience**
   - No need to type email every time
   - Zoho auto-identifies the user
   - @mention support for adding users

3. **Security & Permissions**
   - Only team leads can add/remove users
   - Organization admins can manage teams
   - Clear audit trail

4. **Scalability**
   - Supports multiple organizations
   - Unique team names per organization
   - Easy to add features like team transfers

5. **Data Integrity**
   - No duplicate team names (engineering vs "engineering"})
   - Unique constraint on (org_id, team_name)
   - Proper foreign key relationships

---

## 🚀 Migration Path

1. Add `organizations` table
2. Add `organization_id` to teams table
3. Change primary user identifier from email to `zoho_user_id`
4. Update webhook controller to parse Zoho user info
5. Add permission validation
6. Update all commands to use auto-detected user

---

## 📝 Next Steps

Should I implement this redesign? It will include:

✅ Organization management
✅ Proper team hierarchy  
✅ Auto-user detection (no more typing emails!)
✅ Permission system
✅ @mention support
✅ Better error messages
✅ Team lead approval workflow

This is the professional way to build it! 🎯
