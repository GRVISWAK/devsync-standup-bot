# DevSync Standup Bot

AI-Powered Daily Standup Bot integrated with Zoho Cliq for automated standup management with GitHub, Jira, and Google Calendar integrations.

## 🚀 Features

- **Multi-step Standup Collection**: Interactive bot that asks developers about their work, plans, and blockers
- **AI-Powered Summaries**: Generates concise, professional standup summaries using OpenAI GPT
- **Third-Party Integrations**:
  - 📊 GitHub - Fetches recent commits
  - 🎯 Jira - Retrieves active tasks
  - 📅 Google Calendar - Gets today's events
- **Zoho Cliq Integration**: Seamless integration with slash commands and webhooks
- **Automated Reminders**: Daily scheduled standup reminders
- **Manager Notifications**: Sends formatted summaries to team channels

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Zoho Cliq account with bot permissions
- OpenAI API key
- GitHub Personal Access Token (optional)
- Jira API Token (optional)
- Google Calendar API credentials (optional)

## 🛠️ Tech Stack

- **Backend**: Java Spring Boot 3.2.0
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA with Hibernate
- **AI**: OpenAI GPT-4
- **HTTP Client**: Spring WebFlux
- **Scheduler**: Spring Quartz
- **Build Tool**: Maven

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/devsync/standupbot/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST API controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Exception handlers
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data repositories
│   │   ├── scheduler/       # Scheduled tasks
│   │   ├── service/         # Business logic
│   │   └── util/            # Utility classes
│   └── resources/
│       └── application.properties
└── test/                    # Test files
```

## ⚙️ Configuration

### 1. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE standup_bot;
CREATE USER 'standupuser'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON standup_bot.* TO 'standupuser'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Application Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/standup_bot
spring.datasource.username=standupuser
spring.datasource.password=your_password

# Zoho Cliq
zoho.cliq.webhook.url=https://cliq.zoho.com/api/v2/channelsbyname/YOUR_CHANNEL/message
zoho.cliq.bot.token=YOUR_BOT_TOKEN
zoho.cliq.bot.name=StandupBot

# OpenAI
openai.api.key=sk-your-openai-api-key
openai.model=gpt-4
openai.max.tokens=500

# GitHub (Optional)
github.api.url=https://api.github.com
github.token=ghp_your_github_token

# Jira (Optional)
jira.api.url=https://your-domain.atlassian.net
jira.email=your-email@example.com
jira.api.token=your_jira_api_token

# Standup Reminder (9 AM, Monday-Friday)
standup.reminder.cron=0 0 9 * * MON-FRI
standup.reminder.enabled=true
```

### 3. Zoho Cliq Bot Setup

1. **Create a Bot in Zoho Cliq**:
   - Go to Zoho Cliq > Bots & Tools > Create Bot
   - Name it "StandupBot"
   - Enable Message Handler and Command Handler

2. **Configure Slash Commands**:
   - Command: `/standup`
   - Webhook URL: `https://your-domain.com/api/webhook/cliq/command`
   - Method: POST

3. **Configure Message Handler**:
   - Webhook URL: `https://your-domain.com/api/webhook/cliq/message`
   - Method: POST

4. **Get Bot Token**:
   - Copy the bot token from Zoho Cliq bot settings
   - Add it to `application.properties`

5. **Configure Incoming Webhook**:
   - Create an incoming webhook for your team channel
   - Copy the webhook URL
   - Add it to `application.properties`

## 🚀 Running the Application

### Local Development

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR file
java -jar target/standup-bot-1.0.0.jar
```

The application will start on `http://localhost:8080`

### Using Docker

```bash
# Build Docker image
docker build -t standup-bot:latest .

# Run with Docker Compose
docker-compose up -d
```

## 📡 API Endpoints

### Standup Operations

- `POST /api/standup/start` - Start a new standup session
- `POST /api/standup/submit` - Submit standup response
- `GET /api/standup/user/{email}` - Get user's recent standups
- `GET /api/standup/user/{email}/date/{date}` - Get standup by date
- `GET /api/standup/date/{date}` - Get all standups for a date
- `GET /api/standup/health` - Health check

### Zoho Cliq Webhooks

- `POST /api/webhook/cliq/command` - Handle slash commands
- `POST /api/webhook/cliq/message` - Handle message responses

## 💬 Using the Bot in Zoho Cliq

### Commands

1. **Start Standup**:
   ```
   /standup now
   ```
   or
   ```
   /standup
   ```

2. **View Your Updates**:
   ```
   /myupdates
   ```

3. **Get Help**:
   ```
   /standup help
   ```

### Workflow

1. User types `/standup now` in Zoho Cliq
2. Bot asks: "What did you work on yesterday?"
3. User responds with their answer
4. Bot asks: "What are you planning to work on today?"
5. User responds
6. Bot asks: "Do you have any blockers or challenges?"
7. User responds
8. Bot processes the standup:
   - Fetches GitHub commits
   - Fetches Jira tasks
   - Fetches calendar events
   - Generates AI summary
9. Bot posts formatted summary to team channel

## 🔧 Integration Setup

### GitHub Integration

1. Generate a Personal Access Token:
   - Go to GitHub Settings > Developer settings > Personal access tokens
   - Generate new token with `repo` and `user` scopes
   - Add token to `application.properties`

2. Store GitHub usernames in user profiles:
   ```
   PUT /api/users/{id}/github
   {
     "githubUsername": "username"
   }
   ```

### Jira Integration

1. Generate an API Token:
   - Go to Jira Account Settings > Security > API tokens
   - Create new token
   - Add to `application.properties`

2. Store Jira account IDs in user profiles:
   ```
   PUT /api/users/{id}/jira
   {
     "jiraAccountId": "account-id"
   }
   ```

### Google Calendar (Optional)

1. Set up Google Calendar API credentials
2. Download credentials.json
3. Place in project root
4. Enable in `application.properties`:
   ```
   google.calendar.enabled=true
   ```

## 📊 Database Schema

### Users Table
- id (Primary Key)
- email (Unique)
- name
- zoho_user_id (Unique)
- github_username
- jira_account_id
- created_at
- updated_at

### Standups Table
- id (Primary Key)
- user_id (Foreign Key)
- standup_date
- yesterday_work
- today_plan
- blockers
- status (IN_PROGRESS, COMPLETED, CANCELLED)
- ai_summary
- github_commits (JSON)
- jira_tasks (JSON)
- calendar_events (JSON)
- current_step
- created_at
- updated_at
- submitted_at

## 🔐 Security Considerations

1. **API Keys**: Store sensitive keys in environment variables or secure vault
2. **Database**: Use strong passwords and SSL connections
3. **Webhooks**: Validate webhook signatures from Zoho Cliq
4. **HTTPS**: Always use HTTPS in production
5. **Rate Limiting**: Implement rate limiting for API endpoints

## 🚢 Deployment

### Heroku

```bash
# Login to Heroku
heroku login

# Create app
heroku create standup-bot

# Add MySQL addon
heroku addons:create jawsdb:kitefin

# Set environment variables
heroku config:set OPENAI_API_KEY=your_key
heroku config:set ZOHO_CLIQ_WEBHOOK_URL=your_webhook_url

# Deploy
git push heroku main
```

### AWS Elastic Beanstalk

1. Create application and environment
2. Configure environment variables
3. Deploy JAR file or use Docker

### Docker Deployment

Use the provided `Dockerfile` and `docker-compose.yml`

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## 📈 Monitoring

- Health check endpoint: `/api/standup/health`
- Application logs: Check `logs/` directory
- Database metrics: Monitor MySQL performance

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check MySQL is running
   - Verify credentials in application.properties
   - Ensure database exists

2. **OpenAI API Error**
   - Verify API key is valid
   - Check API quota/billing
   - Ensure network connectivity

3. **Zoho Cliq Not Responding**
   - Verify webhook URLs are correct
   - Check bot token is valid
   - Ensure application is accessible from internet

4. **GitHub/Jira Integration Not Working**
   - Verify tokens are valid
   - Check user has usernames/IDs configured
   - Verify API endpoints are accessible

## 📞 Support

For issues and questions:
- Create an issue on GitHub
- Contact the development team
- Check the documentation

## 🎯 Roadmap

- [ ] Slack integration
- [ ] Microsoft Teams integration
- [ ] Custom standup questions
- [ ] Analytics dashboard
- [ ] Export standups to PDF
- [ ] Multi-language support
- [ ] Voice input support

---

Built with ❤️ using Java Spring Boot and OpenAI
