# Edge Case and Error Handling Test Script
# Tests validation, error handling, and resilience

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Standup Bot - Edge Case Testing" -ForegroundColor Cyan  
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$passCount = 0
$failCount = 0

function Test-Case {
    param($name, $expected, $actual)
    if ($actual -eq $expected) {
        Write-Host "[PASS] $name" -ForegroundColor Green
        $script:passCount++
    } else {
        Write-Host "[FAIL] $name - Expected: $expected, Got: $actual" -ForegroundColor Red
        $script:failCount++
    }
}

# Wait for app
Write-Host "Waiting for application..." -ForegroundColor Yellow
Start-Sleep -Seconds 3
Write-Host ""

# ========================================
# TEST 1: Invalid Email Format
# ========================================
Write-Host "[Test 1] Invalid Email Format" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body '{"userEmail":"not-an-email"}' `
        -ErrorAction Stop
    Test-Case "Should reject invalid email" 400 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should reject invalid email" 400 $statusCode
    if ($statusCode -eq 400) {
        Write-Host "  [INFO] Error message received" -ForegroundColor Gray
    }
}
Write-Host ""

# ========================================
# TEST 2: Missing Required Field
# ========================================
Write-Host "[Test 2] Missing Required Field (userEmail)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body '{"response":"Some answer"}' `
        -ErrorAction Stop
    Test-Case "Should reject missing email" 400 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should reject missing email" 400 $statusCode
}
Write-Host ""

# ========================================
# TEST 3: Non-Existent User
# ========================================
Write-Host "[Test 3] Non-Existent User" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body '{"userEmail":"nonexistent@example.com"}' `
        -ErrorAction Stop
    Test-Case "Should return 404 for missing user" 404 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should return 404 for missing user" 404 $statusCode
    if ($statusCode -eq 404) {
        Write-Host "  [INFO] Resource not found error" -ForegroundColor Gray
    }
}
Write-Host ""

# ========================================
# TEST 4: Submit Without Starting
# ========================================
Write-Host "[Test 4] Submit Answer Without Starting Standup" -ForegroundColor Yellow

# First create a user
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$userBody = @{
    email = "testuser$timestamp@company.com"
    name = "Test User"
    teamId = 1
    role = "MEMBER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method POST `
        -ContentType "application/json" -Body $userBody
    $userEmail = $user.email
    
    # Try to submit without starting
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
            -ContentType "application/json" `
            -Body "{`"userEmail`":`"$userEmail`",`"response`":`"Test answer`"}" `
            -ErrorAction Stop
        Test-Case "Should reject submit without start" 400 200
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Test-Case "Should reject submit without start" 400 $statusCode
        if ($statusCode -eq 400) {
            Write-Host "  [INFO] No active standup error" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "  [SKIP] Could not create test user" -ForegroundColor Yellow
}
Write-Host ""

# ========================================
# TEST 5: Extremely Long Response
# ========================================
Write-Host "[Test 5] Response Exceeding Length Limit" -ForegroundColor Yellow
$longText = "A" * 6000  # Exceeds 5000 char limit

# Create another user for this test
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$userBody = @{
    email = "longtext$timestamp@company.com"
    name = "Long Text User"
    teamId = 1
    role = "MEMBER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method POST `
        -ContentType "application/json" -Body $userBody
    $userEmail = $user.email
    
    # Start standup
    $standup = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`"}"
    
    # Try long response
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
            -ContentType "application/json" `
            -Body "{`"userEmail`":`"$userEmail`",`"response`":`"$longText`"}" `
            -ErrorAction Stop
        Test-Case "Should reject overly long response" 400 200
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Test-Case "Should reject overly long response" 400 $statusCode
    }
} catch {
    Write-Host "  [SKIP] Could not set up test" -ForegroundColor Yellow
}
Write-Host ""

# ========================================
# TEST 6: Non-Existent Standup ID
# ========================================
Write-Host "[Test 6] Fetch Non-Existent Standup" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/standup/999999" -Method GET `
        -ErrorAction Stop
    Test-Case "Should return 404 for missing standup" 404 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should return 404 for missing standup" 404 $statusCode
}
Write-Host ""

# ========================================
# TEST 7: Invalid Team Name (Empty)
# ========================================
Write-Host "[Test 7] Create Team With Empty Name" -ForegroundColor Yellow
try {
    $teamBody = @{
        teamName = ""
        githubToken = "test"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams" -Method POST `
        -ContentType "application/json" -Body $teamBody -ErrorAction Stop
    Test-Case "Should reject empty team name" 400 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should reject empty team name" 400 $statusCode
}
Write-Host ""

# ========================================
# TEST 8: Invalid Time Format for Reminder
# ========================================
Write-Host "[Test 8] Invalid Reminder Time Format" -ForegroundColor Yellow
try {
    $teamBody = @{
        teamName = "Test Team Invalid Time"
        reminderTime = "25:99"  # Invalid time
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams" -Method POST `
        -ContentType "application/json" -Body $teamBody -ErrorAction Stop
    Test-Case "Should reject invalid time format" 400 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Test-Case "Should reject invalid time format" 400 $statusCode
}
Write-Host ""

# ========================================
# TEST 9: Duplicate Standup (Same Day)
# ========================================
Write-Host "[Test 9] Start Standup Twice on Same Day" -ForegroundColor Yellow

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$userBody = @{
    email = "duplicate$timestamp@company.com"
    name = "Duplicate Test User"
    teamId = 1
    role = "MEMBER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method POST `
        -ContentType "application/json" -Body $userBody
    $userEmail = $user.email
    
    # Start first standup
    $standup1 = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`"}"
    
    # Try to start second standup
    $standup2 = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`"}"
    
    # Should resume existing standup (same ID)
    if ($standup1.standupId -eq $standup2.standupId) {
        Test-Case "Should resume existing standup" "PASS" "PASS"
        Write-Host "  [INFO] Resumed standup ID: $($standup1.standupId)" -ForegroundColor Gray
    } else {
        Test-Case "Should resume existing standup" "PASS" "FAIL"
    }
} catch {
    Write-Host "  [SKIP] Could not set up test" -ForegroundColor Yellow
}
Write-Host ""

# ========================================
# TEST 10: Complete Standup with Missing Integrations
# ========================================
Write-Host "[Test 10] Complete Standup Without GitHub/Jira Configured" -ForegroundColor Yellow

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$userBody = @{
    email = "nointeg$timestamp@company.com"
    name = "No Integration User"
    teamId = 1
    role = "MEMBER"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method POST `
        -ContentType "application/json" -Body $userBody
    $userEmail = $user.email
    
    # Start standup
    $standup = Invoke-RestMethod -Uri "$baseUrl/api/standup/start" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`"}"
    
    # Submit all answers
    Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`",`"response`":`"Yesterday work`"}" | Out-Null
    
    Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`",`"response`":`"Today plan`"}" | Out-Null
    
    Invoke-RestMethod -Uri "$baseUrl/api/standup/submit" -Method POST `
        -ContentType "application/json" `
        -Body "{`"userEmail`":`"$userEmail`",`"response`":`"No blockers`"}" | Out-Null
    
    # Wait for async processing
    Start-Sleep -Seconds 2
    
    # Fetch standup
    $final = Invoke-RestMethod -Uri "$baseUrl/api/standup/$($standup.standupId)" -Method GET
    
    if ($final.status -eq "COMPLETED") {
        Test-Case "Should complete without integrations" "PASS" "PASS"
        Write-Host "  [INFO] Gracefully handled missing integrations" -ForegroundColor Gray
    } else {
        Test-Case "Should complete without integrations" "PASS" "FAIL"
    }
} catch {
    Write-Host "  [ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Test-Case "Should complete without integrations" "PASS" "FAIL"
}
Write-Host ""

# ========================================
# SUMMARY
# ========================================
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Total Tests: $($passCount + $failCount)" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red

if ($failCount -eq 0) {
    Write-Host ""
    Write-Host "[PASS] All edge case tests passed!" -ForegroundColor Green
    Write-Host "   The application handles errors gracefully." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[WARN] Some tests failed. Review error handling." -ForegroundColor Yellow
}
Write-Host "================================================" -ForegroundColor Cyan
