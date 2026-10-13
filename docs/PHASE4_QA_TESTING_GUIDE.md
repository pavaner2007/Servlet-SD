# QA Test Execution Guide: Phase 4 Caching, Messaging & Notifications

**Document ID:** `QA-P4-LMS-2026-V1.0`  
**Author:** Senior QA Test & Infrastructure Engineer  
**Scope:** Redis Write-Through Caching, RabbitMQ Event-Driven Architecture, Notification Service, and Overdue Reminder Cron Job

---

## 1. Architecture & Flow Overview

```
 [PUT /api/v1/loans/return]
            │
            ├─► 1. Update Database (MySQL: loans = RETURNED, copy = AVAILABLE)
            │
            ├─► 2. Redis Write-Through Invalidation:
            │      - SET book:availability:{id} = newCount (Write-Through)
            │      - DEL catalog:search:*                  (Invalidation)
            │
            └─► 3. Message Broker (RabbitMQ):
                   - Publish BookReturnedEvent
                            │
                            ▼
                   Notification Service (Consumer)
                   - Checks reservation queue for next member
                   - Allocates copy to top reservation
                   - Dispatches & logs Email/SMS in 'notification_logs'

 [Scheduled / Manual Trigger]
            │
            ▼
 Overdue Reminder Cron Job
 - Queries loans WHERE due_date < NOW()
 - Computes accrued fines ($0.50/day)
 - Dispatches & logs Overdue Email/SMS
```

---

## 2. Test Section 1: Redis Cache & Write-Through Invalidation Verification

### 2.1 Monitor Redis in Real-Time
Open a terminal window and attach the Redis real-time monitor:
```bash
redis-cli MONITOR
```

### 2.2 Test Step 1: Initial Catalogue Query (Cache MISS)
Run:
```bash
curl -i -X GET "http://localhost:8080/Servlet_SD/api/v1/catalog/search?q=Effective&onlyAvailable=true"
```
* **Expected HTTP Header:** `X-Cache: MISS`
* **Redis Verification:**
  ```bash
  redis-cli KEYS "catalog:search:*"
  redis-cli GET "book:availability:1"
  ```
  *Output:* A cached search key has been created with TTL 600s, and `book:availability:1` is cached.

### 2.3 Test Step 2: Repeat Query (Cache HIT)
Run the exact same curl request:
```bash
curl -i -X GET "http://localhost:8080/Servlet_SD/api/v1/catalog/search?q=Effective&onlyAvailable=true"
```
* **Expected HTTP Header:** `X-Cache: HIT`
* **Redis Monitor Log:** Shows `GET catalog:search:...` executed without MySQL database hit.

### 2.4 Test Step 3: Trigger Book Return (Write-Through Invalidation)
Return the loaned copy:
```bash
curl -i -X PUT "http://localhost:8080/Servlet_SD/api/v1/loans/return" \
     -H "Content-Type: application/json" \
     -d '{"barcode": "BC-EJ-001"}'
```
* **Redis Monitor Log Output:**
  ```text
  15:30:12 [0 127.0.0.1:54321] "SETEX" "book:availability:1" "3600" "2"
  15:30:12 [0 127.0.0.1:54321] "KEYS" "catalog:search:*"
  15:30:12 [0 127.0.0.1:54321] "DEL" "catalog:search:q=Effective:genre=*:cat=*:avail=true:p=1:l=10"
  ```
* **Assertion:**
  1. `redis-cli GET "book:availability:1"` immediately reflects the updated available count (**Write-Through**).
  2. `redis-cli KEYS "catalog:search:*"` returns `(empty list or set)` (**Eviction / Invalidation**).

### 2.5 Test Step 4: Verify Eviction Effect
Re-run the search query:
```bash
curl -i -X GET "http://localhost:8080/Servlet_SD/api/v1/catalog/search?q=Effective&onlyAvailable=true"
```
* **Expected HTTP Header:** `X-Cache: MISS` (proving stale cache was evicted and fresh data was loaded from database).

---

## 3. Test Section 2: Event-Driven Architecture & Notification Service

### 3.1 Verify Message Broker Dispatch
When a book is returned:
1. `MessagePublisher` publishes `BookReturnedEvent` to RabbitMQ exchange `library.events.exchange` with routing key `library.event.book.returned`.
2. Inspect with RabbitMQ Management CLI / Web Console (`http://localhost:15672`):
   ```bash
   rabbitmqadmin get queue=library.events.book-returned.queue count=1
   ```
   Or observe application server log:
   ```text
   📤 [RABBITMQ PUBLISHED] Event ID: d2a498b7-6b45-4bb8-8c1d... -> Exchange: 'library.events.exchange'
   ```

### 3.2 Verify Notification Consumer & Email/SMS Logging
The `NotificationService` consumes the event, inspects the reservation queue for `bookId = 1`, and alerts the next waiting member.

**Check Application Console:**
```text
📥 [EVENT CONSUMED] Processing BookReturnedEvent for Book ID: 1, Copy: BC-EJ-001
📧 [EMAIL DISPATCHED] To: bob.johnson@gmail.com | Subject: Library Alert: Your reserved book 'Effective Java' is ready for pickup!
📱 [SMS DISPATCHED] To: +1-555-0102 | Message: Library Alert: 'Effective Java' is now available! Held for Bob Johnson (Res #1)...
```

**Check Database Table (MySQL):**
```sql
SELECT notification_id, member_id, channel, recipient, subject, trigger_event, dispatch_status, dispatched_at 
FROM notification_logs 
ORDER BY notification_id DESC 
LIMIT 5;
```

**Expected DB Output:**
```text
+-----------------+-----------+---------+-----------------------+-----------------------------+---------------------------+-----------------+
| notification_id | member_id | channel | recipient             | subject                     | trigger_event             | dispatch_status |
+-----------------+-----------+---------+-----------------------+-----------------------------+---------------------------+-----------------+
|               1 |         2 | EMAIL   | bob.johnson@gmail.com | Library Alert: Your res...  | BOOK_RETURNED_ALLOCATION  | SENT            |
|               2 |         2 | SMS     | +1-555-0102           | SMS Alert                   | BOOK_RETURNED_ALLOCATION  | SENT            |
+-----------------+-----------+---------+-----------------------+-----------------------------+---------------------------+-----------------+
```

---

## 4. Test Section 3: Overdue Due-Date Reminder Cron Job

### 4.1 Automated Schedule
The cron job (`OverdueReminderCronJob`) automatically scans every 24 hours.

### 4.2 Manual On-Demand QA Trigger
To test and demonstrate the cron job immediately without waiting for the timer, trigger the Admin REST endpoint:

```bash
curl -X POST "http://localhost:8080/Servlet_SD/api/v1/admin/jobs/trigger-overdue-reminders" \
     -H "Accept: application/json"
```

**Expected Response Payload:**
```json
{
  "status": "SUCCESS",
  "scannedOverdueLoans": 1,
  "noticesDispatched": 1,
  "totalFinesAccrued": 3.50,
  "executedAt": "2026-10-13T10:00:00Z"
}
```

**Verify Application Log:**
```text
⏰ [CRON JOB STARTED] Scanning for overdue library loans...
🚨 [OVERDUE EMAIL] Dispatched to bob.johnson@gmail.com for item 'Head First Design Patterns' (Due: 2026-10-07, Fine: $3.50)
🚨 [OVERDUE SMS] Dispatched to +1-555-0102
✅ [CRON JOB COMPLETED] Scanned: 1 overdue loans | Notices Sent: 1 | Total Fines: $3.50
```

**Verify in MySQL Database:**
```sql
-- 1. Verify accrued fine updated in loans table:
SELECT loan_id, member_id, status, fine_amount, due_date FROM loans WHERE status = 'OVERDUE';

-- 2. Verify audit logs created:
SELECT * FROM notification_logs WHERE trigger_event = 'OVERDUE_REMINDER';
```

---

## 5. Automated Single-Command Test Execution

You can run the full automated verification test in a single command:

**PowerShell (Windows):**
```powershell
.\testing\test_phase4_caching_messaging.ps1
```

**Bash (Linux / Git Bash):**
```bash
chmod +x ./testing/test_phase4_caching_messaging.sh
./testing/test_phase4_caching_messaging.sh
```
