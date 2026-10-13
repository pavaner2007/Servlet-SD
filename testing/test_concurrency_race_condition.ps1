# ==============================================================================
# QA Concurrency Stress Test: Optimistic Locking Verification (PowerShell)
# Simulates two members attempting to checkout the same copy barcode simultaneously.
# Expected Outcome: Exactly ONE 201 Created and Exactly ONE 409 Conflict.
# ==============================================================================

$BaseUrl = "http://localhost:8080/Servlet_SD"
$Barcode = "BC-EJ-001"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting QA Concurrency Race Condition Test: Barcode: $Barcode" -ForegroundColor Cyan
Write-Host " Endpoint: POST $BaseUrl/api/v1/loans/checkout" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$ScriptBlock = {
    param($BaseUrl, $MemberId, $Barcode)
    $Uri = "$BaseUrl/api/v1/loans/checkout"
    $Body = @{
        memberId = $MemberId
        barcode  = $Barcode
        version  = 1
        durationDays = 14
    } | ConvertTo-Json

    try {
        $response = Invoke-WebRequest -Uri $Uri -Method POST -Body $Body -ContentType "application/json" -ErrorAction Stop
        return [PSCustomObject]@{
            MemberId   = $MemberId
            StatusCode = [int]$response.StatusCode
            Content    = $response.Content
        }
    }
    catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $content = $reader.ReadToEnd()
        return [PSCustomObject]@{
            MemberId   = $MemberId
            StatusCode = $statusCode
            Content    = $content
        }
    }
}

# Start Member 1 and Member 2 simultaneously as background jobs
$Job1 = Start-Job -ScriptBlock $ScriptBlock -ArgumentList $BaseUrl, 1, $Barcode
$Job2 = Start-Job -ScriptBlock $ScriptBlock -ArgumentList $BaseUrl, 2, $Barcode

# Wait for both jobs
$Results = Wait-Job $Job1, $Job2 | Receive-Job
Remove-Job $Job1, $Job2

Write-Host "`n--- Execution Results ---" -ForegroundColor Yellow
$Results | ForEach-Object {
    $color = if ($_.StatusCode -eq 201) { "Green" } elseif ($_.StatusCode -eq 409) { "DarkYellow" } else { "Red" }
    Write-Host "Member $($_.MemberId): HTTP $($_.StatusCode)" -ForegroundColor $color
    Write-Host "$($_.Content)`n"
}

$SuccessCount  = ($Results | Where-Object { $_.StatusCode -eq 201 }).Count
$ConflictCount = ($Results | Where-Object { $_.StatusCode -eq 409 }).Count

Write-Host "--- Assertion Verification ---" -ForegroundColor Yellow
if ($SuccessCount -eq 1 -and $ConflictCount -eq 1) {
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host " [TEST PASSED]: Optimistic Locking verified successfully!" -ForegroundColor Green
    Write-Host " - 1 Request succeeded with HTTP 201 Created" -ForegroundColor Green
    Write-Host " - 1 Competing Request failed with HTTP 409 Conflict" -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Green
} else {
    Write-Host "======================================================================" -ForegroundColor Red
    Write-Host " [TEST FAILED]: Concurrency violation or unexpected HTTP codes" -ForegroundColor Red
    Write-Host " 201 Created: $SuccessCount (Expected: 1)" -ForegroundColor Red
    Write-Host " 409 Conflict: $ConflictCount (Expected: 1)" -ForegroundColor Red
    Write-Host "======================================================================" -ForegroundColor Red
}
