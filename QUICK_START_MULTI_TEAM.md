# Quick Start: Multi-Team Standup Bot

## 🚀 Your Application is Running!

The multi-team standup bot is now running on **http://localhost:8080**

## 📋 Quick Test Steps

### Step 1: Create a Team

```bash
curl -X POST http://localhost:8080/api/admin/teams \
  -H "Content-Type: application/json" \
  -d '{
    "teamName": "Engineering Team",
    "githubToken": "ghp_your_github_token_here",
    "githubOrg": "your-org",
    "jiraApiUrl": "https://your-domain.atlassian.net",
    "jiraEmail": "your-email@company.com",
    "jiraApiToken": "your_jira_api_token",
    "openaiApiKey": "sk-your_openai_key",
    "zohoWebhookUrl": "https://cliq.zoho.com/api/v2/channelsbyname/your-channel/message",
    "zohoChannelId": "your-channel-id",
    "reminderEnabled": true,
    "reminderTime": "09:00"
  }'
```

### Step 2: Create a User in the Team

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "developer@company.com",
    "name": "John Developer",
    "zohoUserId": "john_dev",
    "teamId": 1,
    "role": "MEMBER"
  }'
```

### Step 3: Configure User Integrations (Optional)

Users can optionally use personal tokens instead of team tokens:

```bash
curl -X POST http://localhost:8080/api/users/integrations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "teamId": 1,
    "githubUsername": "john-dev",
    "githubPersonalToken": "ghp_personal_token",
    "jiraAccountId": "account-id",
    "jiraPersonalToken": "personal_jira_token",
    "useTeamTokens": false
  }'
```

### Step 4: Start a Standup

```bash
curl -X POST http://localhost:8080/api/standups/start \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "developer@company.com"
  }'
```

### Step 5: Submit Answers

```bash
curl -X POST http://localhost:8080/api/standups/submit \
  -H "Content-Type: application/json" \
  -d '{
    "standupId": 1,
    "answer": "Worked on authentication module and fixed login bugs"
  }'
```

```bash
curl -X POST http://localhost:8080/api/standups/submit \
  -H "Content-Type: application/json" \
  -d '{
    "standupId": 1,
    "answer": "Will implement password reset functionality"
  }'
```

```bash
curl -X POST http://localhost:8080/api/standups/submit \
  -H "Content-Type: application/json" \
  -d '{
    "standupId": 1,
    "answer": "No blockers"
  }'
```

## 🔑 Getting API Tokens

### GitHub Token
1. Go to GitHub Settings → Developer Settings → Personal Access Tokens
2. Generate new token (classic)
3. Select scopes: `repo`, `read:user`
4. Copy the token (starts with `ghp_`)

### Jira Token
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Give it a name and copy the token

### OpenAI API Key
1. Go to https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Copy the key (starts with `sk-`)

### Zoho Cliq Webhook
1. Open Zoho Cliq
2. Go to the channel where you want standup updates
3. Click on the channel menu → Bots & Integrations → Incoming Webhook
4. Create webhook and copy the URL

## 📊 View All Teams

```bash
curl http://localhost:8080/api/admin/teams
```

## 🔄 Multi-Team Deployment Strategies

### 1. Centralized (Team-Level Tokens)
- Team admin configures all API tokens in team settings
- All team members use the same tokens
- Best for: Small teams, unified integrations

### 2. Distributed (User-Level Tokens)
- Each user provides their own personal tokens
- Maximum security and personalization
- Best for: Large teams, strict security requirements

### 3. Hybrid (Mix Both)
- Team provides default tokens
- Users can optionally override with personal tokens
- Best for: Flexible deployment, gradual adoption

## ✅ What's Working Now

- ✅ Multi-team support
- ✅ Team-level API token management
- ✅ User-level integration settings
- ✅ Role-based access (ADMIN/MANAGER/MEMBER)
- ✅ GitHub commit fetching
- ✅ Jira task integration
- ✅ OpenAI summary generation
- ✅ Zoho Cliq notifications
- ✅ Daily reminders via scheduler

## 📖 Full Documentation

See `DEPLOYMENT_GUIDE.md` for complete workplace deployment instructions.

## 🎉 Success!

Your standup bot is now ready for multi-team workplace deployment! Each team can configure their own API tokens and start using the bot independently.
