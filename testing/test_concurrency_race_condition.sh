#!/usr/bin/env bash
# ==============================================================================
# QA Concurrency Stress Test: Optimistic Locking Verification
# Simulates two members attempting to checkout the same copy barcode simultaneously.
# Expected Outcome: Exactly ONE 201 Created and Exactly ONE 409 Conflict.
# ==============================================================================

BASE_URL="http://localhost:8080/Servlet_SD"
BARCODE="BC-EJ-001"
RESP_FILE_MEMBER1="/tmp/checkout_member1.json"
RESP_FILE_MEMBER2="/tmp/checkout_member2.json"
HTTP_CODE_MEMBER1="/tmp/http_code_1.txt"
HTTP_CODE_MEMBER2="/tmp/http_code_2.txt"

echo "======================================================================"
echo " Starting QA Concurrency Race Condition Test: Barcode: ${BARCODE}"
echo " Endpoint: POST ${BASE_URL}/api/v1/loans/checkout"
echo "======================================================================"

# Reset copy in database if running locally (optional pre-condition)
# UPDATE physical_copies SET status = 'AVAILABLE', version = 1 WHERE barcode = 'BC-EJ-001';

# Launch Member 1 Checkout Request in Background
(
  HTTP_STATUS=$(curl -s -o "${RESP_FILE_MEMBER1}" -w "%{http_code}" -X POST "${BASE_URL}/api/v1/loans/checkout" \
    -H "Content-Type: application/json" \
    -d "{
      \"memberId\": 1,
      \"barcode\": \"${BARCODE}\",
      \"version\": 1,
      \"durationDays\": 14
    }")
  echo "${HTTP_STATUS}" > "${HTTP_CODE_MEMBER1}"
) &
PID1=$!

# Launch Member 2 Competing Checkout Request in Background simultaneously
(
  HTTP_STATUS=$(curl -s -o "${RESP_FILE_MEMBER2}" -w "%{http_code}" -X POST "${BASE_URL}/api/v1/loans/checkout" \
    -H "Content-Type: application/json" \
    -d "{
      \"memberId\": 2,
      \"barcode\": \"${BARCODE}\",
      \"version\": 1,
      \"durationDays\": 14
    }")
  echo "${HTTP_STATUS}" > "${HTTP_CODE_MEMBER2}"
) &
PID2=$!

# Await completion of both simultaneous background workers
wait $PID1 $PID2

STATUS_1=$(cat "${HTTP_CODE_MEMBER1}" 2>/dev/null || echo "000")
STATUS_2=$(cat "${HTTP_CODE_MEMBER2}" 2>/dev/null || echo "000")

echo -e "\n--- Response Summary ---"
echo "Member 1 (PID ${PID1}) HTTP Response Code: ${STATUS_1}"
cat "${RESP_FILE_MEMBER1}" 2>/dev/null
echo ""

echo "Member 2 (PID ${PID2}) HTTP Response Code: ${STATUS_2}"
cat "${RESP_FILE_MEMBER2}" 2>/dev/null
echo ""

echo "--- QA Assertion Analysis ---"

SUCCESS_COUNT=0
CONFLICT_COUNT=0

if [ "${STATUS_1}" -eq 201 ]; then ((SUCCESS_COUNT++)); fi
if [ "${STATUS_2}" -eq 201 ]; then ((SUCCESS_COUNT++)); fi

if [ "${STATUS_1}" -eq 409 ]; then ((CONFLICT_COUNT++)); fi
if [ "${STATUS_2}" -eq 409 ]; then ((CONFLICT_COUNT++)); fi

if [ "${SUCCESS_COUNT}" -eq 1 ] && [ "${CONFLICT_COUNT}" -eq 1 ]; then
    echo "======================================================================"
    echo " [TEST PASSED]: Optimistic locking successfully prevented double checkout!"
    echo " - Winner Transaction: Received 201 Created"
    echo " - Loser Transaction: Received 409 Conflict (OPTIMISTIC_LOCK_CONFLICT)"
    echo "======================================================================"
    exit 0
else
    echo "======================================================================"
    echo " [TEST FAILED]: Unexpected outcome under concurrency!"
    echo " - 201 Created count: ${SUCCESS_COUNT} (Expected: 1)"
    echo " - 409 Conflict count: ${CONFLICT_COUNT} (Expected: 1)"
    echo "======================================================================"
    exit 1
fi
