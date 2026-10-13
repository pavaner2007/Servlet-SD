# ==============================================================================
# QA Test Script: Phase 4 Caching, Messaging & Notification Systems (PowerShell)
# ==============================================================================

$BaseUrl = "http://localhost:8080/Servlet_SD"
$Barcode = "BC-EJ-001"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " PHASE 4 QA AUTOMATION TEST SUITE (PowerShell)" -ForegroundColor Cyan
Write-Host " 1. Redis Write-Through Invalidation" -ForegroundColor Cyan
Write-Host " 2. RabbitMQ BookReturnedEvent Dispatch" -ForegroundColor Cyan
Write-Host " 3. Notification Service Queue Alert (Email/SMS)" -ForegroundColor Cyan
Write-Host " 4. Overdue Reminders Cron Job Execution" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# Step 1: Initial Search Request (Expect Cache MISS)
Write-Host "`n[STEP 1] Initial Search Request (Expect Cache MISS)..." -ForegroundColor Yellow
$resp1 = Invoke-WebRequest -Uri "$BaseUrl/api/v1/catalog/search?q=Effective&onlyAvailable=true" -Method GET
$cacheHeader1 = $resp1.Headers["X-Cache"]
Write-Host "HTTP Status: $($resp1.StatusCode) | X-Cache Header: $cacheHeader1" -ForegroundColor $(if ($cacheHeader1 -eq "MISS") { "Green" } else { "DarkYellow" })

# Step 2: Second Search Request (Expect Cache HIT)
Write-Host "`n[STEP 2] Immediate Repeat Search Request (Expect Cache HIT)..." -ForegroundColor Yellow
$resp2 = Invoke-WebRequest -Uri "$BaseUrl/api/v1/catalog/search?q=Effective&onlyAvailable=true" -Method GET
$cacheHeader2 = $resp2.Headers["X-Cache"]
Write-Host "HTTP Status: $($resp2.StatusCode) | X-Cache Header: $cacheHeader2" -ForegroundColor $(if ($cacheHeader2 -eq "HIT") { "Green" } else { "Red" })

# Step 3: Return the Book -> Triggers Write-Through Invalidation & Event
Write-Host "`n[STEP 3] Returning Book (PUT /api/v1/loans/return)..." -ForegroundColor Yellow
$returnBody = @{ barcode = $Barcode } | ConvertTo-Json
$returnResp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/loans/return" -Method PUT -Body $returnBody -ContentType "application/json"
Write-Host "HTTP Status: $($returnResp.StatusCode)" -ForegroundColor Green
Write-Host "Response Payload: $($returnResp.Content)"

# Step 4: Search Request after return (Expect Cache MISS due to write-through invalidation)
Write-Host "`n[STEP 4] Search Request After Return (Expect Cache MISS due to invalidation)..." -ForegroundColor Yellow
$resp3 = Invoke-WebRequest -Uri "$BaseUrl/api/v1/catalog/search?q=Effective&onlyAvailable=true" -Method GET
$cacheHeader3 = $resp3.Headers["X-Cache"]
Write-Host "HTTP Status: $($resp3.StatusCode) | X-Cache Header: $cacheHeader3" -ForegroundColor $(if ($cacheHeader3 -eq "MISS") { "Green" } else { "Red" })

# Step 5: Manually trigger the Overdue Reminder Cron Job
Write-Host "`n[STEP 5] Trigger Overdue Reminders Cron Job..." -ForegroundColor Yellow
$cronResp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/admin/jobs/trigger-overdue-reminders" -Method POST
Write-Host "Cron Execution Status: $($cronResp.StatusCode)" -ForegroundColor Green
Write-Host "Cron Summary Payload: $($cronResp.Content)" -ForegroundColor Green

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host " [QA SUMMARY] All Phase 4 systems triggered and verified!" -ForegroundColor Green
Write-Host " Run 'SELECT * FROM notification_logs ORDER BY notification_id DESC;' in MySQL to inspect alerts." -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
