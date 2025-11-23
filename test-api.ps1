# Test API Script for DevSync Standup Bot
# PowerShell script to test all API endpoints

$baseUrl = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DevSync Standup Bot - API Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/health" -Method GET
    Write-Host "✓ Health check passed" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json) -ForegroundColor Gray
} catch {
    Write-Host "✗ Health check failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Start Standup
Write-Host "Test 2: Start Standup" -ForegroundColor Yellow
$startStandupBody = @{
    zohoUserId = "test123"
    userEmail = "test@example.com"
    userName = "Test User"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" -Body $startStandupBody
    Write-Host "✓ Standup started successfully" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json) -ForegroundColor Gray
    $standupId = $response.standupId
} catch {
    Write-Host "✗ Start standup failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Submit Response - Step 1
Write-Host "Test 3: Submit Response - Yesterday's Work" -ForegroundColor Yellow
$submitBody1 = @{
    userEmail = "test@example.com"
    response = "Worked on API integration and database setup"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" -Body $submitBody1
    Write-Host "✓ Response submitted successfully" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json) -ForegroundColor Gray
} catch {
    Write-Host "✗ Submit response failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Submit Response - Step 2
Write-Host "Test 4: Submit Response - Today's Plan" -ForegroundColor Yellow
$submitBody2 = @{
    userEmail = "test@example.com"
    response = "Will work on Zoho Cliq integration and testing"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" -Body $submitBody2
    Write-Host "✓ Response submitted successfully" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json) -ForegroundColor Gray
} catch {
    Write-Host "✗ Submit response failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 5: Submit Response - Step 3
Write-Host "Test 5: Submit Response - Blockers" -ForegroundColor Yellow
$submitBody3 = @{
    userEmail = "test@example.com"
    response = "No blockers at the moment"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" -Body $submitBody3
    Write-Host "✓ Response submitted successfully" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json) -ForegroundColor Gray
} catch {
    Write-Host "✗ Submit response failed: $_" -ForegroundColor Red
}
Write-Host ""

# Wait for async processing
Write-Host "Waiting for async processing (5 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Test 6: Get User Standups
Write-Host "Test 6: Get User Standups" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/user/test@example.com?limit=5" -Method GET
    Write-Host "✓ Retrieved user standups" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5) -ForegroundColor Gray
} catch {
    Write-Host "✗ Get user standups failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 7: Get Standup by Date
Write-Host "Test 7: Get Standup by Date" -ForegroundColor Yellow
$today = Get-Date -Format "yyyy-MM-dd"
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/user/test@example.com/date/$today" -Method GET
    Write-Host "✓ Retrieved standup by date" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5) -ForegroundColor Gray
} catch {
    Write-Host "✗ Get standup by date failed: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Tests Completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
