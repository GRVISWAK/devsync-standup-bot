# DevSync Standup Bot - Railway Deployment Guide

## Quick Deploy to Railway (Free Tier)

### Step 1: Prepare Repository
```bash
# Ensure .env is in .gitignore (already done)
# Push your code to GitHub
git add .
git commit -m "Ready for Railway deployment"
git push origin main
```

### Step 2: Deploy to Railway

1. **Go to**: https://railway.app/
2. **Sign in** with GitHub
3. **Click**: "New Project" → "Deploy from GitHub repo"
4. **Select**: Your DevSync repo
5. **Railway will auto-detect** Spring Boot and create a service

### Step 3: Add MySQL Database

1. In your Railway project, click **"+ New"** → **"Database"** → **"MySQL"**
2. Railway will auto-create the database
3. Copy the connection details

### Step 4: Set Environment Variables

In Railway dashboard → Your service → **Variables** tab, add:

```env
# Database (Railway provides these automatically as DATABASE_URL)
SPRING_DATASOURCE_URL=${DATABASE_URL}
SPRING_DATASOURCE_USERNAME=${MYSQLUSER}
SPRING_DATASOURCE_PASSWORD=${MYSQLPASSWORD}

# Zoho Cliq (REQUIRED)
ZOHO_CLIQ_WEBHOOK_URL=https://cliq.zoho.com/api/v2/channelsbyname/YOUR_CHANNEL/message
ZOHO_CLIQ_BOT_TOKEN=your_actual_bot_token
ZOHO_CLIQ_BOT_NAME=StandupBot

# OpenAI (REQUIRED for AI summaries)
OPENAI_API_KEY=sk-your-actual-openai-key

# GitHub (Optional)
GITHUB_TOKEN=ghp_your_token

# Jira (Optional)
JIRA_API_URL=https://your-domain.atlassian.net
JIRA_EMAIL=your@email.com
JIRA_API_TOKEN=your_token
```

### Step 5: Get Your Public URL

1. Railway auto-generates a URL like: `https://your-app.up.railway.app`
2. Go to **Settings** → **Networking** → **Generate Domain**
3. Copy this URL - you'll use it in Zoho Cliq

### Step 6: Configure Zoho Cliq Bot

1. Go to Zoho Cliq → **Bots** → Your bot
2. Set **Webhook URL** to: `https://your-app.up.railway.app/api/zoho/webhook`
3. Save and test!

---

## Railway Free Tier Limits
- ✅ 500 hours/month (enough for demo)
- ✅ MySQL database included
- ✅ Auto-deploy on git push
- ✅ HTTPS by default
- ✅ No credit card required

## Test Your Deployment

```bash
# Health check
curl https://your-app.up.railway.app/api/standup/health

# Expected response:
# {"status":"UP","service":"DevSync Standup Bot","timestamp":"..."}
```

---

## Troubleshooting

**App won't start?**
- Check Railway logs: Dashboard → Deployments → View Logs
- Verify all required env variables are set
- Ensure MySQL database is connected

**Zoho webhook not working?**
- Verify webhook URL in Zoho Cliq matches Railway domain
- Check `/api/zoho/webhook` endpoint is accessible
- Review application logs for incoming requests

**Database connection failed?**
- Railway auto-sets `DATABASE_URL` variable
- Make sure Spring datasource URL uses Railway's MySQL

---

## Alternative: Render.com Deployment

If Railway doesn't work, try Render.com:

1. **Go to**: https://render.com
2. **New** → **Web Service** → Connect GitHub repo
3. **Build Command**: `mvn clean package -DskipTests`
4. **Start Command**: `java -jar target/standup-bot-1.0.0.jar`
5. Add **PostgreSQL** database (free tier)
6. Set same environment variables as above

---

**After deployment, you're ready to record your demo video! 🎥**
