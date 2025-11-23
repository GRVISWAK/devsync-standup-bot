# Script to wait for app and run tests
Write-Host "Waiting for application to be ready on port 8080..." -ForegroundColor Yellow

$maxAttempts = 30
$attempt = 0
$appReady = $false

while ($attempt -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/admin/teams" -Method GET -TimeoutSec 2 -ErrorAction Stop
        $appReady = $true
        Write-Host "Application is ready!" -ForegroundColor Green
        break
    } catch {
        $attempt++
        Write-Host "Attempt $attempt/$maxAttempts - App not ready yet..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}

if (-not $appReady) {
    Write-Host "Application did not start in time. Please check if mvn spring-boot:run is running." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting tests..." -ForegroundColor Cyan
Write-Host ""

# Run the actual test
& "$PSScriptRoot\test-bot.ps1"
