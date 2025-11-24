# Test Railway Deployment
# Replace YOUR_RAILWAY_URL with your actual Railway domain

$RAILWAY_URL = "https://devsync-standup-bot-production.up.railway.app"

Write-Host "Testing Railway Deployment..." -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$RAILWAY_URL/api/standup/health" -Method Get
    Write-Host "✅ Health Check: " -ForegroundColor Green -NoNewline
    Write-Host $health.status
} catch {
    Write-Host "❌ Health Check Failed: $_" -ForegroundColor Red
}

# Test 2: Create Team
Write-Host "`n2. Creating Test Team..." -ForegroundColor Yellow
$teamBody = @{
    name = "TestTeam"
    channelId = "test-channel-001"
    timezone = "UTC"
} | ConvertTo-Json

try {
    $team = Invoke-RestMethod -Uri "$RAILWAY_URL/api/teams" -Method Post -Body $teamBody -ContentType "application/json"
    Write-Host "✅ Team Created: ID = $($team.id)" -ForegroundColor Green
    $teamId = $team.id
} catch {
    Write-Host "❌ Team Creation Failed: $_" -ForegroundColor Red
}

# Test 3: Register User
Write-Host "`n3. Registering Test User..." -ForegroundColor Yellow
$userBody = @{
    username = "testuser"
    email = "testuser@example.com"
    zohoUserId = "test-zoho-123"
    role = "DEVELOPER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "$RAILWAY_URL/api/users" -Method Post -Body $userBody -ContentType "application/json"
    Write-Host "✅ User Registered: ID = $($user.id)" -ForegroundColor Green
    $userId = $user.id
} catch {
    Write-Host "❌ User Registration Failed: $_" -ForegroundColor Red
}

# Test 4: Submit Standup
Write-Host "`n4. Submitting Test Standup..." -ForegroundColor Yellow
$standupBody = @{
    userId = $userId
    teamId = $teamId
    yesterdayWork = "Completed user authentication module"
    todayPlan = "Working on standup bot integration with Zoho Cliq"
    blockers = "None"
    workHours = 8
    mood = "HAPPY"
} | ConvertTo-Json

try {
    $standup = Invoke-RestMethod -Uri "$RAILWAY_URL/api/standup/submit" -Method Post -Body $standupBody -ContentType "application/json"
    Write-Host "✅ Standup Submitted Successfully!" -ForegroundColor Green
    Write-Host "Summary: $($standup.aiSummary)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Standup Submission Failed: $_" -ForegroundColor Red
}

Write-Host "`n✅ Railway Deployment Test Complete!" -ForegroundColor Green
