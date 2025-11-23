# 🚀 Quick Start Guide for Workplace Deployment

## For Team Administrators

### Step 1: Configure Your Team (5 minutes)

```bash
# Configure your team with all API tokens
curl -X POST http://localhost:8080/api/admin/teams \
  -H "Content-Type: application/json" \
  -d '{
    "teamName": "Engineering",
    "zohoChannelId": "your_channel_id",
    "zohoWebhookUrl": "https://cliq.zoho.com/api/v2/channelsbyname/engineering/message?zapikey=xxx",
    
    "githubOrganization": "your-company",
    "githubToken": "ghp_your_github_org_token",
    
    "jiraApiUrl": "https://yourcompany.atlassian.net",
    "jiraEmail": "bot@yourcompany.com",
    "jiraApiToken": "ATATT3xFfGF0_your_jira_token",
    
    "openaiApiKey": "sk-proj-your_openai_key",
    "openaiModel": "gpt-4",
    
    "reminderEnabled": true,
    "reminderTime": "09:00"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Team configuration saved successfully",
  "teamId": 1,
  "teamName": "Engineering"
}
```

### Step 2: Share with Your Team

Send this message to your team channel:

```
🎉 Standup Bot is now available!

To get started:
1. Type /standup setup in this channel
2. Follow the bot's instructions to link your GitHub and Jira accounts
3. Start using /standup now for daily standups!

Need help? Type /standup help
```

---

## For Team Members

### Step 1: Link Your Accounts (1 minute)

```bash
# Simple setup - using team tokens
curl -X POST http://localhost:8080/api/users/integrations \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "you@company.com",
    "teamId": 1,
    "githubUsername": "your-github-username",
    "jiraAccountId": "your-jira-account-id",
    "useTeamTokens": true
  }'
```

**Don't know your Jira account ID?**
- Go to Jira → Click your profile picture → Profile
- Look at the URL: `...accountId=XXXXX` - that's your account ID

### Step 2: Start Using the Bot

In Zoho Cliq, type:
```
/standup now
```

Answer the 3 questions and get an AI-generated summary!

---

## API Endpoints Reference

### Team Management (Admins Only)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/admin/teams` | POST | Create/update team config |
| `/api/admin/teams` | GET | List all teams |
| `/api/admin/teams/{id}` | GET | Get specific team |
| `/api/admin/teams/{id}` | DELETE | Delete team |
| `/api/admin/teams/{id}/setup-guide` | GET | Get setup instructions |

### User Integration (All Users)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/users/integrations` | POST | Configure integrations |
| `/api/users/integrations/setup-guide` | GET | Get setup instructions |

### Standup Operations

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/standup/start` | POST | Start new standup |
| `/api/standup/submit` | POST | Submit answer |
| `/api/standup/user/{email}` | GET | Get user's standups |
| `/api/standup/health` | GET | Health check |

---

## Testing the Setup

### Test 1: Verify Team Configuration

```bash
curl http://localhost:8080/api/admin/teams
```

Expected: List of configured teams

### Test 2: Get Setup Guide

```bash
curl http://localhost:8080/api/users/integrations/setup-guide
```

Expected: Detailed instructions with examples

### Test 3: Complete Standup Flow

```bash
# 1. Start standup
curl -X POST http://localhost:8080/api/standup/start \
  -H "Content-Type: application/json" \
  -d '{
    "zohoUserId": "123",
    "userEmail": "test@company.com",
    "userName": "Test User"
  }'

# 2. Submit yesterday's work
curl -X POST http://localhost:8080/api/standup/submit \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "test@company.com",
    "response": "Worked on API integration"
  }'

# 3. Submit today's plan
curl -X POST http://localhost:8080/api/standup/submit \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "test@company.com",
    "response": "Will implement multi-team support"
  }'

# 4. Submit blockers
curl -X POST http://localhost:8080/api/standup/submit \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "test@company.com",
    "response": "No blockers"
  }'

# Wait 5 seconds for processing

# 5. Get the standup
curl http://localhost:8080/api/standup/user/test@company.com
```

---

## Security Checklist

Before deploying to production:

- [ ] Change database password from default
- [ ] Enable HTTPS/SSL
- [ ] Implement API authentication (JWT recommended)
- [ ] Set up token encryption for stored credentials
- [ ] Configure rate limiting
- [ ] Set up logging and monitoring
- [ ] Create backup strategy
- [ ] Document disaster recovery plan
- [ ] Set up alerts for failed API calls
- [ ] Review and limit database access

---

## Troubleshooting

### Issue: "Team not found"
**Solution:** Make sure you've created the team first using POST `/api/admin/teams`

### Issue: "GitHub commits not showing"
**Solution:** 
1. Verify GitHub token has correct permissions
2. Check user has set their GitHub username
3. Verify user has commits in last 24 hours

### Issue: "Jira tasks not showing"
**Solution:**
1. Verify Jira token and credentials
2. Check user's Jira account ID is correct
3. Verify user has active tasks assigned

### Issue: "AI summary not generated"
**Solution:**
1. Check OpenAI API key is valid
2. Verify OpenAI account has credits
3. Check logs for specific API errors

---

## Production Deployment Checklist

- [ ] Set up production database (MySQL/PostgreSQL)
- [ ] Configure environment variables (don't use application.properties)
- [ ] Set up reverse proxy (nginx/Apache)
- [ ] Configure SSL certificate
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK stack)
- [ ] Set up automated backups
- [ ] Configure auto-scaling (if cloud)
- [ ] Set up CI/CD pipeline
- [ ] Document runbook for operations team

---

## Support

For issues or questions:
- Check logs: `logs/application.log`
- Review documentation: `README.md`, `MULTI_TEAM_GUIDE.md`
- Contact your system administrator

---

**Last Updated:** November 22, 2025  
**Version:** 2.0.0 (Multi-Team Support)
