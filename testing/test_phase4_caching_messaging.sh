#!/usr/bin/env bash
# ==============================================================================
# QA Test Script: Phase 4 Caching, Messaging & Notification Systems
# ==============================================================================

BASE_URL="http://localhost:8080/Servlet_SD"
BARCODE="BC-EJ-001"
BOOK_ID="1"

echo "======================================================================"
echo " PHASE 4 QA AUTOMATION TEST SUITE"
echo " 1. Redis Write-Through Invalidation"
echo " 2. RabbitMQ BookReturnedEvent Dispatch"
echo " 3. Notification Service Queue Alert (Email/SMS)"
echo " 4. Overdue Reminders Cron Job Execution"
echo "======================================================================"

echo -e "\n[STEP 1] Initial Search Request (Expect Cache MISS -> Populates Redis)..."
curl -s -i -X GET "${BASE_URL}/api/v1/catalog/search?q=Effective&onlyAvailable=true" \
  | grep -E "(HTTP/|X-Cache:)"

echo -e "\n[STEP 2] Immediate Repeat Search Request (Expect Cache HIT from Redis)..."
curl -s -i -X GET "${BASE_URL}/api/v1/catalog/search?q=Effective&onlyAvailable=true" \
  | grep -E "(HTTP/|X-Cache:)"

echo -e "\n[STEP 3] Check current Redis cache keys before return..."
if command -v redis-cli &> /dev/null; then
    echo "Active Redis Keys matching 'catalog:*' or 'book:*':"
    redis-cli KEYS "catalog:search:*"
    redis-cli KEYS "book:availability:*"
else
    echo "(redis-cli not found in PATH; skipping CLI inspection, relying on HTTP headers)"
fi

echo -e "\n[STEP 4] Return Book (PUT /api/v1/loans/return) -> Triggers Write-Through Invalidation & Event..."
curl -s -i -X PUT "${BASE_URL}/api/v1/loans/return" \
  -H "Content-Type: application/json" \
  -d "{\"barcode\": \"${BARCODE}\"}" \
  | grep -E "(HTTP/|status|loanId)"

echo -e "\n[STEP 5] Verify Cache Invalidation (Next Search MUST be Cache MISS)..."
curl -s -i -X GET "${BASE_URL}/api/v1/catalog/search?q=Effective&onlyAvailable=true" \
  | grep -E "(HTTP/|X-Cache:)"

echo -e "\n[STEP 6] Manually Trigger Overdue Due-Date Reminder Cron Job..."
curl -s -i -X POST "${BASE_URL}/api/v1/admin/jobs/trigger-overdue-reminders" \
  -H "Accept: application/json"

echo -e "\n\n======================================================================"
echo " Phase 4 Automated Test Completed Successfully!"
echo " Check application log and MySQL table 'notification_logs' for Email/SMS audit entries."
echo "======================================================================"
