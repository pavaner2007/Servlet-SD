# QA Test Execution Guide: Phase 3 REST APIs & Concurrency Suite

**Document ID:** `QA-P3-LMS-2026-V1.0`  
**Author:** Lead QA Automation Engineer  
**Scope:** Circulation, Membership, and Catalogue Microservices  
**Focus Area:** Optimistic Locking verification & Race Condition handling on scarce copies (`409 Conflict`)

---

## 1. REST Endpoints Specification

| Endpoint | Method | Microservice | Description | Success Code | Conflict Code |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/api/v1/catalog/search` | `GET` | Catalogue | Search catalog by title, author, genre, category, and availability | `200 OK` | N/A |
| `/api/v1/reservations` | `POST` | Circulation | Places a hold queue reservation for scarce book copies | `201 Created` | `403 Forbidden` |
| `/api/v1/loans/checkout` | `POST` | Circulation | Checks out a physical copy using **Optimistic Locking** | `201 Created` | **`409 Conflict`** |
| `/api/v1/loans/return` | `PUT` | Circulation | Returns a loaned copy, increments stock, notifies queue | `200 OK` | `404 Not Found` |

---

## 2. Optimistic Locking Architecture

### Problem Statement
When two members attempt to check out the **last available copy** of a popular book at the same time, without concurrency controls, both transactions could succeed, leading to a duplicate checkout (phantom read / double loan).

### Concurrency Solution
1. `physical_copies` table contains a `version INT NOT NULL DEFAULT 1` column.
2. The checkout service performs an atomic, version-guarded update:
   ```sql
   UPDATE physical_copies 
   SET status = 'LOANED', version = version + 1, updated_at = CURRENT_TIMESTAMP 
   WHERE barcode = ? AND status = 'AVAILABLE' AND version = ?;
   ```
3. If `rowsAffected == 0`, a competing transaction has already modified the row.
4. The service halts and throws `OptimisticLockConflictException`, which the Servlet translates into:
   - **HTTP Status:** `409 Conflict`
   - **Error Code:** `OPTIMISTIC_LOCK_CONFLICT`
   - **Payload Details:**
     ```json
     {
       "status": 409,
       "errorCode": "OPTIMISTIC_LOCK_CONFLICT",
       "message": "Optimistic lock conflict: Copy 'BC-EJ-001' was modified concurrently by another transaction...",
       "details": {
         "barcode": "BC-EJ-001",
         "attemptedVersion": 1,
         "currentVersion": 2,
         "currentStatus": "LOANED",
         "resolution": "Please refresh the copy details and re-attempt checkout or join waitlist."
       }
     }
     ```

---

## 3. Test Suites Provided

All test scripts are located in the [`testing/`](file:///c:/Servlet_SD/testing/) folder:

1. **Postman Collection:** `testing/Library_API_Postman_Collection.json`
2. **Curl Suite:** `testing/curl_requests.sh`
3. **Bash Concurrency Stress Script:** `testing/test_concurrency_race_condition.sh`
4. **PowerShell Concurrency Script:** `testing/test_concurrency_race_condition.ps1`
5. **Apache JMeter Test Plan:** `testing/jmeter_concurrency_test_plan.jmx`

---

## 4. Step-by-Step QA Test Execution

### 4.1 Running the Postman Collection
1. Open **Postman** -> Click **Import** -> Select `testing/Library_API_Postman_Collection.json`.
2. Ensure variable `baseUrl` is configured (default: `http://localhost:8080/Servlet_SD`).
3. Click **Run Collection** (or run with Newman CLI):
   ```bash
   newman run testing/Library_API_Postman_Collection.json
   ```
4. Verify all tests pass with green assertions (`200 OK`, `201 Created`, `409 Conflict`).

---

### 4.2 Running the Concurrency Race Condition Test

#### Option A: Windows PowerShell (Native)
Run directly in PowerShell without installing bash or WSL:
```powershell
.\testing\test_concurrency_race_condition.ps1
```

**Expected Console Output:**
```text
======================================================================
 Starting QA Concurrency Race Condition Test: Barcode: BC-EJ-001
 Endpoint: POST http://localhost:8080/Servlet_SD/api/v1/loans/checkout
======================================================================

--- Execution Results ---
Member 1: HTTP 201
{"loanId":4,"memberId":1,"copyId":1,"loanType":"PHYSICAL","status":"ACTIVE",...}

Member 2: HTTP 409
{"status":409,"errorCode":"OPTIMISTIC_LOCK_CONFLICT","message":"Optimistic lock conflict...",...}

--- Assertion Verification ---
======================================================================
 [TEST PASSED]: Optimistic Locking verified successfully!
 - 1 Request succeeded with HTTP 201 Created
 - 1 Competing Request failed with HTTP 409 Conflict
======================================================================
```

#### Option B: Linux / Git Bash
```bash
chmod +x testing/test_concurrency_race_condition.sh
./testing/test_concurrency_race_condition.sh
```

---

### 4.3 Running with Apache JMeter
1. Launch Apache JMeter: `jmeter`
2. Open file: `testing/jmeter_concurrency_test_plan.jmx`
3. Configure `BASE_URL` (`localhost`) and `PORT` (`8080`) if needed.
4. Click **Start** (Green play button).
5. Open **View Results Tree**:
   - Thread 1 displays green with HTTP `201 Created`.
   - Thread 2 displays green under assertion with HTTP `409 Conflict`.

---

## 5. Database Verification (MySQL)

Run the following SQL queries to verify table state after the concurrency test:

```sql
-- 1. Verify physical_copies version was incremented exactly once and status is LOANED
SELECT copy_id, barcode, status, version 
FROM physical_copies 
WHERE barcode = 'BC-EJ-001';
-- Expected: status = 'LOANED', version = 2

-- 2. Verify exactly ONE active loan was created
SELECT loan_id, member_id, copy_id, status 
FROM loans 
WHERE copy_id = 1 AND status = 'ACTIVE';
-- Expected: 1 row only (either member_id = 1 or member_id = 2)

-- 3. Verify available_physical_copies in catalog was decremented by 1
SELECT book_id, title, total_physical_copies, available_physical_copies 
FROM book_titles 
WHERE book_id = 1;
```
