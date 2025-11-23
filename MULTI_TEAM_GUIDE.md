# 🏢 Multi-Team Deployment Guide

## Architecture Overview

The bot now supports **multiple teams** with separate configurations:

- ✅ Each team has its own API tokens (GitHub, Jira, OpenAI)
- ✅ Each team has its own Zoho Cliq channel
- ✅ Users can belong to multiple teams
- ✅ Admins can configure team settings via API
- ✅ Users can use team tokens OR personal tokens

---

## Database Schema

### New Tables

**teams** - Stores team-level configuration
- Team name, Zoho channel
- GitHub org token
- Jira API credentials  
- OpenAI API key
- Settings (reminders, etc.)

**user_integrations** - Stores user-level tokens
- Links users to teams
- Optional personal GitHub/Jira tokens
- Falls back to team tokens if not provided

**users** - Enhanced with team relationship
- Added `team_id` foreign key
- Added `role` (ADMIN, MANAGER, MEMBER)

---

## Setup for Workplace Deployment

### Step 1: Admin Configures Team

**POST** `/api/admin/teams`

```json
{
  "teamName": "Engineering Team",
  "zohoChannelId": "channel_123",
  "zohoWebhookUrl": "https://cliq.zoho.com/api/v2/channelsbyname/engineering/message?zapikey=xxx",
  
  "githubOrganization": "your-company",
  "githubToken": "ghp_company_token_here",
  
  "jiraApiUrl": "https://yourcompany.atlassian.net",
  "jiraEmail": "bot@yourcompany.com",
  "jiraApiToken": "ATATT3xFfGF0_company_token",
  
  "openaiApiKey": "sk-proj-company_key",
  "openaiModel": "gpt-4",
  
  "reminderEnabled": true,
  "reminderTime": "09:00"
}
```

### Step 2: Users Link Their Accounts

**Option A: Use Team Tokens (Default)**

Users just need to provide their usernames/IDs:

```json
{
  "userEmail": "john@company.com",
  "teamId": 1,
  "githubUsername": "johndoe",
  "jiraAccountId": "5f8a1b2c3d4e5f6g7h8i9j0k",
  "useTeamTokens": true
}
```

**Option B: Use Personal Tokens**

Users provide their own API tokens:

```json
{
  "userEmail": "john@company.com",
  "teamId": 1,
  "githubUsername": "johndoe",
  "githubPersonalToken": "ghp_johns_personal_token",
  "jiraAccountId": "5f8a1b2c3d4e5f6g7h8i9j0k",
  "jiraPersonalToken": "johns_jira_token",
  "useTeamTokens": false
}
```

---

## Token Strategy Options

### Strategy 1: Centralized (Recommended for Most Companies)

**Who manages tokens:** IT Admin / DevOps  
**Tokens stored:** Team level  
**User setup:** Just username/account ID

**Pros:**
- ✅ Easy user onboarding
- ✅ Centralized token management
- ✅ Better security control
- ✅ Easier token rotation

**Cons:**
- ❌ Requires admin access to get org tokens
- ❌ All API calls use same token (rate limits)

**Best for:** Small to medium companies (5-100 developers)

### Strategy 2: Distributed

**Who manages tokens:** Each individual user  
**Tokens stored:** User level  
**User setup:** Each user adds their tokens

**Pros:**
- ✅ No admin overhead
- ✅ Users control their own tokens
- ✅ Better rate limit distribution
- ✅ More granular permissions

**Cons:**
- ❌ More complex user onboarding
- ❌ Users need to generate tokens
- ❌ Harder to manage/rotate

**Best for:** Large companies (100+ developers), consulting firms

### Strategy 3: Hybrid (Most Flexible)

**Who manages tokens:** Admin provides defaults, users can override  
**Tokens stored:** Both team and user level  
**User setup:** Optional personal tokens

**Pros:**
- ✅ Best of both worlds
- ✅ Users can opt-in to personal tokens
- ✅ Fallback to team tokens
- ✅ Flexible for different use cases

**Cons:**
- ❌ More complex implementation
- ❌ Need to handle fallback logic

**Best for:** Any size company wanting flexibility

---

## Deployment Scenarios

### Scenario A: Single Company, Multiple Teams

```
Company: Acme Corp
Teams:
  - Engineering (50 devs)
  - QA (20 testers)  
  - DevOps (10 engineers)

Setup:
1. Create 3 team configs
2. Each team gets own GitHub org token
3. Each team has own Jira project
4. Each team has own Zoho Cliq channel
5. Users specify team when using /standup
```

### Scenario B: Multiple Companies (SaaS)

```
Customers:
  - Company A (100 devs)
  - Company B (50 devs)
  - Company C (200 devs)

Setup:
1. Each company is a "team"
2. Each company provides their tokens
3. Completely isolated
4. Multi-tenant database
5. Each company has own Zoho Cliq workspace
```

---

## API Endpoints for Team Management

### Admin Endpoints

```bash
# Create/Update Team
POST /api/admin/teams
Body: TeamConfigRequest

# Get All Teams
GET /api/admin/teams

# Get Team by ID
GET /api/admin/teams/{teamId}

# Delete Team
DELETE /api/admin/teams/{teamId}

# Get Setup Guide
GET /api/admin/teams/{teamId}/setup-guide
```

### User Integration Endpoints

```bash
# Configure User Integration
POST /api/users/integrations
Body: UserIntegrationRequest

# Get User Integration
GET /api/users/{email}/integrations

# Update User Integration
PUT /api/users/{email}/integrations
```

---

## Token Acquisition Guide for Admins

### For GitHub Organization Token

1. Go to GitHub Organization Settings
2. Settings → Developer settings → Personal access tokens → Tokens (classic)
3. Generate token with scopes:
   - `repo` (all)
   - `read:org`
   - `read:user`
4. Name it: `Standup Bot - [Team Name]`
5. Set expiration: 90 days (create calendar reminder to rotate)
6. Save token to team config

### For Jira Organization Token

1. Create a "bot" user account in Jira (e.g., `standup-bot@company.com`)
2. Give it read access to all relevant projects
3. Log in as bot user
4. Go to https://id.atlassian.com/manage-profile/security/api-tokens
5. Create API token
6. Save token to team config

### For OpenAI Organization Key

**Option A: Shared Key (Simple)**
1. Use company OpenAI account
2. Set spending limits ($50/month recommended)
3. Monitor usage dashboard

**Option B: Per-Team Keys (Better)**
1. Create sub-accounts for each team
2. Each team gets own budget
3. Better cost tracking

---

## User Onboarding Flow

### For Team Token Strategy:

**Step 1:** User receives welcome message in Zoho Cliq

**Step 2:** User types `/standup setup`

**Step 3:** Bot asks for:
- GitHub username
- Jira account ID (bot can look this up automatically)

**Step 4:** Done! User can start using `/standup now`

### For Personal Token Strategy:

**Step 1-2:** Same as above

**Step 3:** Bot provides links:
- "Get GitHub token: https://github.com/settings/tokens"
- "Get Jira token: https://id.atlassian.com/manage-profile/security/api-tokens"

**Step 4:** User pastes tokens (sent via DM, not in channel)

**Step 5:** Bot confirms and saves securely

---

## Security Considerations

### Token Encryption

Tokens should be encrypted at rest. Add encryption service:

```java
// TODO: Implement token encryption
@Service
public class TokenEncryptionService {
    public String encrypt(String token) { /* Use AES-256 */ }
    public String decrypt(String encrypted) { /* Use AES-256 */ }
}
```

### Access Control

- Only ADMIN role can configure team settings
- Users can only see their own tokens
- Tokens never returned in API responses (except during setup)

### Audit Logging

Log all token access:
- When tokens are created/updated
- When tokens are used for API calls
- Failed authentication attempts

---

## Example: Complete Setup for Engineering Team

```bash
# 1. Admin creates team (one-time)
curl -X POST http://localhost:8080/api/admin/teams \
  -H "Content-Type: application/json" \
  -d '{
    "teamName": "Engineering",
    "zohoChannelId": "eng_channel_123",
    "zohoWebhookUrl": "https://cliq.zoho.com/api/v2/channelsbyname/engineering/message?zapikey=xxx",
    "githubOrganization": "acmecorp",
    "githubToken": "ghp_AcmeCorpEngineeringToken",
    "jiraApiUrl": "https://acmecorp.atlassian.net",
    "jiraEmail": "bot@acmecorp.com",
    "jiraApiToken": "ATATT3xFfGF0xxx",
    "openaiApiKey": "sk-proj-xxx",
    "reminderEnabled": true,
    "reminderTime": "09:00"
  }'

# 2. User links their account (per user)
curl -X POST http://localhost:8080/api/users/integrations \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "john@acmecorp.com",
    "teamId": 1,
    "githubUsername": "johndoe",
    "jiraAccountId": "5f8a1b2c3d4e5f6g",
    "useTeamTokens": true
  }'

# 3. User starts using bot in Zoho Cliq
# Just type: /standup now
```

---

## Migration from Single-Team to Multi-Team

If you've already deployed the single-team version:

1. Create a "Default" team with your current tokens
2. Migrate existing users to this team
3. Add new teams as needed
4. Update `application.properties` to have fallback values

---

## Next Implementation Steps

I've created the models and controllers. Next we need to:

1. ✅ Update `StandupService` to use team-based tokens
2. ✅ Update `GitHubService` to accept team parameter
3. ✅ Update `JiraService` to accept team parameter
4. ✅ Update `AIService` to use team's OpenAI key
5. ✅ Add token encryption
6. ✅ Add user onboarding flow
7. ✅ Update Zoho webhook handler to detect team

Should I implement these enhancements now?
