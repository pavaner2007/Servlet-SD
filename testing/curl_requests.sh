#!/usr/bin/env bash
# ==============================================================================
# Library Management System - REST API Curl Test Suite (Phase 3)
# ==============================================================================

BASE_URL="http://localhost:8080/Servlet_SD"
BARCODE="BC-EJ-001"

echo "=========================================================="
echo "1. GET /api/v1/catalog/search"
echo "=========================================================="
curl -s -X GET "${BASE_URL}/api/v1/catalog/search?q=Java&onlyAvailable=true&page=1&limit=5" \
     -H "Accept: application/json" | jq .

echo -e "\n=========================================================="
echo "2. POST /api/v1/reservations"
echo "=========================================================="
curl -s -i -X POST "${BASE_URL}/api/v1/reservations" \
     -H "Content-Type: application/json" \
     -d '{
       "memberId": 1,
       "bookId": 2,
       "holdDays": 5
     }'

echo -e "\n=========================================================="
echo "3. POST /api/v1/loans/checkout (Member 1 Checkout)"
echo "=========================================================="
curl -s -i -X POST "${BASE_URL}/api/v1/loans/checkout" \
     -H "Content-Type: application/json" \
     -d "{
       \"memberId\": 1,
       \"barcode\": \"${BARCODE}\",
       \"version\": 1,
       \"durationDays\": 14
     }"

echo -e "\n=========================================================="
echo "4. POST /api/v1/loans/checkout (Member 2 Competing Checkout -> 409 Conflict)"
echo "=========================================================="
curl -s -i -X POST "${BASE_URL}/api/v1/loans/checkout" \
     -H "Content-Type: application/json" \
     -d "{
       \"memberId\": 2,
       \"barcode\": \"${BARCODE}\",
       \"version\": 1,
       \"durationDays\": 14
     }"

echo -e "\n=========================================================="
echo "5. PUT /api/v1/loans/return (Return Book)"
echo "=========================================================="
curl -s -i -X PUT "${BASE_URL}/api/v1/loans/return" \
     -H "Content-Type: application/json" \
     -d "{
       \"barcode\": \"${BARCODE}\"
     }"
