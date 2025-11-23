# DevSync Standup Bot - Setup Guide

## Quick Start Guide

### Prerequisites Checklist
- [ ] Java 17 installed
- [ ] Maven 3.6+ installed
- [ ] MySQL 8.0+ installed and running
- [ ] Zoho Cliq account with admin access
- [ ] OpenAI API account and API key

### Step 1: Clone and Setup Project

```bash
cd d:\Devsync
mvn clean install
```

### Step 2: Database Setup

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database and user
CREATE DATABASE standup_bot;
CREATE USER 'standupuser'@'localhost' IDENTIFIED BY 'StandupPass123!';
GRANT ALL PRIVILEGES ON standup_bot.* TO 'standupuser'@'localhost';
FLUSH PRIVILEGES;
```

### Step 3: Configure Application

1. Copy the environment template:
```bash
copy .env.example .env
```

2. Edit `.env` and fill in your values:
   - OpenAI API key
   - Zoho Cliq webhook URL and bot token
   - Database credentials
   - Optional: GitHub token, Jira credentials

3. Update `application.properties` if needed

### Step 4: Setup Zoho Cliq Bot

#### Create Bot
1. Log into Zoho Cliq
2. Go to **Bots & Tools** > **Bots** > **Create Bot**
3. Fill in:
   - Bot Name: `StandupBot`
   - Description: `AI-powered daily standup assistant`
   - Enable: Message Handler and Command Handler

#### Configure Slash Command
1. In bot settings, go to **Command**
2. Add command: `/standup`
3. Set webhook URL: `https://your-domain.com/api/webhook/cliq/command`
4. Method: `POST`
5. Save

#### Configure Message Handler
1. In bot settings, go to **Message Handler**
2. Set webhook URL: `https://your-domain.com/api/webhook/cliq/message`
3. Method: `POST`
4. Save

#### Create Incoming Webhook
1. Go to your team channel
2. Click **Settings** > **Integrations** > **Incoming Webhook**
3. Create webhook named "Standup Summaries"
4. Copy the webhook URL
5. Add to `.env` as `ZOHO_CLIQ_WEBHOOK_URL`

#### Get Bot Token
1. In bot settings, find **Bot Unique Token**
2. Copy the token
3. Add to `.env` as `ZOHO_CLIQ_BOT_TOKEN`

### Step 5: Get API Keys

#### OpenAI API Key
1. Go to https://platform.openai.com/
2. Navigate to **API Keys**
3. Create new secret key
4. Copy and add to `.env` as `OPENAI_API_KEY`

#### GitHub Token (Optional)
1. Go to GitHub Settings > Developer settings > Personal access tokens
2. Generate new token (classic)
3. Select scopes: `repo`, `user`
4. Copy and add to `.env` as `GITHUB_TOKEN`

#### Jira API Token (Optional)
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Create API token
3. Copy and add to `.env` as `JIRA_API_TOKEN`
4. Also set `JIRA_API_URL` and `JIRA_EMAIL`

### Step 6: Run the Application

#### Local Development
```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/standup-bot-1.0.0.jar
```

#### Using Docker
```bash
# Build and run with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f app

# Stop
docker-compose down
```

### Step 7: Test the Bot

1. Open Zoho Cliq
2. Go to your team channel
3. Type `/standup help` to verify bot is responding
4. Type `/standup now` to start a test standup

### Step 8: Configure Ngrok (for local testing)

If testing locally, you need to expose your local server:

```bash
# Install ngrok
# Download from https://ngrok.com/

# Start ngrok
ngrok http 8080

# Copy the HTTPS URL (e.g., https://abc123.ngrok.io)
# Update Zoho Cliq webhook URLs with this ngrok URL
```

## Deployment Options

### Option 1: Heroku

```bash
# Login to Heroku
heroku login

# Create app
heroku create your-standup-bot

# Add JawsDB MySQL addon
heroku addons:create jawsdb:kitefin

# Get database URL
heroku config:get JAWSDB_URL

# Set config vars
heroku config:set OPENAI_API_KEY=your_key
heroku config:set ZOHO_CLIQ_WEBHOOK_URL=your_webhook
heroku config:set ZOHO_CLIQ_BOT_TOKEN=your_token

# Deploy
git push heroku main

# Check logs
heroku logs --tail
```

### Option 2: AWS Elastic Beanstalk

```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p java-17 standup-bot

# Create environment
eb create standup-bot-env

# Set environment variables
eb setenv OPENAI_API_KEY=your_key ZOHO_CLIQ_WEBHOOK_URL=your_webhook

# Deploy
eb deploy

# Check status
eb status
```

### Option 3: Google Cloud Platform

```bash
# Build JAR
mvn clean package

# Deploy to App Engine
gcloud app deploy

# View logs
gcloud app logs tail -s default
```

### Option 4: Digital Ocean

1. Create a Droplet (Ubuntu 22.04)
2. Install Java 17 and MySQL
3. Clone repository
4. Configure application.properties
5. Run with systemd service

## Post-Deployment Steps

### 1. Update Zoho Cliq Webhooks
Replace ngrok/localhost URLs with your production domain:
- Command webhook: `https://your-domain.com/api/webhook/cliq/command`
- Message webhook: `https://your-domain.com/api/webhook/cliq/message`

### 2. Test All Features
- [ ] `/standup now` command works
- [ ] Multi-step questions appear
- [ ] Responses are saved
- [ ] AI summary is generated
- [ ] Summary posted to channel
- [ ] `/myupdates` shows history
- [ ] Daily reminder is sent

### 3. Configure User Integrations

Users can link their accounts via API:

```bash
# Link GitHub username
curl -X PUT https://your-domain.com/api/users/1/github \
  -H "Content-Type: application/json" \
  -d '{"githubUsername": "johndoe"}'

# Link Jira account
curl -X PUT https://your-domain.com/api/users/1/jira \
  -H "Content-Type: application/json" \
  -d '{"jiraAccountId": "account-id-here"}'
```

### 4. Monitor Application

```bash
# Health check
curl https://your-domain.com/api/standup/health

# Check logs
tail -f logs/application.log
```

## Troubleshooting

### Issue: Bot doesn't respond to commands
- Verify webhook URLs are correct and accessible
- Check bot token is valid
- Ensure application is running
- Check application logs for errors

### Issue: Database connection failed
- Verify MySQL is running
- Check credentials in application.properties
- Ensure database `standup_bot` exists
- Check firewall rules

### Issue: OpenAI API errors
- Verify API key is correct
- Check account has credits
- Verify internet connectivity
- Check rate limits

### Issue: GitHub/Jira integration not working
- Verify tokens are valid and have correct permissions
- Check user has linked their accounts
- Verify API endpoints are accessible
- Check logs for specific errors

## Security Best Practices

1. **Never commit sensitive data**
   - Add `.env` to `.gitignore`
   - Use environment variables for secrets

2. **Use HTTPS in production**
   - Get SSL certificate (Let's Encrypt)
   - Configure reverse proxy (nginx/Apache)

3. **Implement rate limiting**
   - Protect API endpoints
   - Prevent abuse

4. **Regular updates**
   - Keep dependencies updated
   - Monitor security advisories

5. **Database security**
   - Use strong passwords
   - Enable SSL connections
   - Regular backups

## Support

For issues or questions:
- Check logs: `logs/application.log`
- Review README.md for detailed documentation
- Create GitHub issue
- Contact development team

## Next Steps

1. Customize standup questions (edit `StandupService.java`)
2. Add custom reminder messages
3. Configure timezone for reminders
4. Set up monitoring and alerts
5. Create analytics dashboard
6. Add more integrations

---

Happy standup automating! 🚀
