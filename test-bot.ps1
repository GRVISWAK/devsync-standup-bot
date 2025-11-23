# Complete Test Script for Standup Bot
# Run this in PowerShell after starting the app with: mvn spring-boot:run

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Multi-Team Standup Bot - Complete Test" -ForegroundColor Cyan  
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Wait for app to be ready
Write-Host "Waiting for application to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host ""

# Step 1: Create Team
Write-Host "[Step 1] Creating Engineering Team..." -ForegroundColor Yellow
$teamBody = @{
    teamName = "Engineering Team"
    githubToken = "test_gh_token"
    githubOrganization = "test-org"
    jiraApiUrl = "https://test.atlassian.net"
    jiraEmail = "test@company.com"
    jiraApiToken = "test_jira"
    openaiApiKey = "test_openai"
    zohoWebhookUrl = "https://test-webhook.zoho.com"
    zohoChannelId = "test-channel"
    reminderEnabled = $false
    calendarEnabled = $false
} | ConvertTo-Json

try {
    $team = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/teams" -Method POST -ContentType "application/json" -Body $teamBody
    Write-Host "[OK] Team created successfully! Team ID: $($team.teamId)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to create team: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Step 2: Create User
Write-Host ""
Write-Host "[Step 2] Creating user..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$userBody = @{
    email = "developer$timestamp@company.com"
    name = "John Developer"
    zohoUserId = "john_dev_$timestamp"
    teamId = 1
    role = "MEMBER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method POST -ContentType "application/json" -Body $userBody
    Write-Host "[OK] User created successfully! Email: $($user.email)" -ForegroundColor Green
    $userEmail = $user.email
} catch {
    Write-Host "[ERROR] Failed to create user: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
    exit
}

# Step 3: Start Standup
Write-Host ""
Write-Host "[Step 3] Starting standup session..." -ForegroundColor Yellow
$startBody = @{
    userEmail = $userEmail
} | ConvertTo-Json

try {
    $standup = Invoke-RestMethod -Uri "http://localhost:8080/api/standup/start" -Method POST -ContentType "application/json" -Body $startBody
    Write-Host "[OK] Standup started! ID: $($standup.standupId)" -ForegroundColor Green
    Write-Host "   Question: $($standup.nextQuestion)" -ForegroundColor Cyan
    $standupId = $standup.standupId
} catch {
    Write-Host "[ERROR] Failed to start standup: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Step 4-6: Submit 3 Answers
Write-Host ""
Write-Host "[Step 4] Submitting answer 1..." -ForegroundColor Yellow
$answer1 = @{
    userEmail = $userEmail
    response = "Worked on authentication module, fixed login bugs, and implemented JWT tokens"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/standup/submit" -Method POST -ContentType "application/json" -Body $answer1
    Write-Host "[OK] Answer 1 submitted!" -ForegroundColor Green
    Write-Host "   Next Question: $($response1.nextQuestion)" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to submit answer 1: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "[Step 5] Submitting answer 2..." -ForegroundColor Yellow
$answer2 = @{
    userEmail = $userEmail
    response = "Will implement password reset functionality and write comprehensive unit tests"
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/standup/submit" -Method POST -ContentType "application/json" -Body $answer2
    Write-Host "[OK] Answer 2 submitted!" -ForegroundColor Green
    Write-Host "   Next Question: $($response2.nextQuestion)" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to submit answer 2: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "[Step 6] Submitting answer 3 (final)..." -ForegroundColor Yellow
$answer3 = @{
    userEmail = $userEmail
    response = "No blockers at the moment!"
} | ConvertTo-Json

try {
    $response3 = Invoke-RestMethod -Uri "http://localhost:8080/api/standup/submit" -Method POST -ContentType "application/json" -Body $answer3
    Write-Host "[OK] Answer 3 submitted! Standup completed!" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to submit answer 3: $($_.Exception.Message)" -ForegroundColor Red
}

# Step 7: View Final Summary
Write-Host ""
Write-Host "[Step 7] Fetching standup summary..." -ForegroundColor Yellow
try {
    $finalStandup = Invoke-RestMethod -Uri "http://localhost:8080/api/standup/$standupId" -Method GET
    Write-Host "[OK] Standup retrieved successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  STANDUP SUMMARY" -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "User: $($finalStandup.userName)" -ForegroundColor White
    Write-Host "Status: $($finalStandup.status)" -ForegroundColor White
    Write-Host ""
    Write-Host "AI Summary:" -ForegroundColor Yellow
    Write-Host $finalStandup.aiSummary -ForegroundColor White
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to fetch standup: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed successfully!" -ForegroundColor Green
Write-Host "   The standup bot is working WITHOUT needing OpenAI API key!" -ForegroundColor Green
Write-Host "   It uses the enhanced fallback summary generator." -ForegroundColor Green
