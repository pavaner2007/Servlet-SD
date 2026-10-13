# QA Test Execution Script: User Management Module (Servlet & JDBC)

**Document Reference:** `QA-TC-UMM-2026-V1.0`  
**Author:** Senior QA Test Engineer  
**Target Module:** User Management Module (`UserServlet.java` + `index.html`)  
**Database Under Test:** MySQL (`user_auth_db.users`)  
**Protocol / Method:** HTTP POST over `/UserServlet`

---

## 1. Test Environment Setup & Preconditions

### 1.1 Database Precondition
Ensure MySQL service is active and the table is initialized:
```sql
CREATE DATABASE IF NOT EXISTS user_auth_db;
USE user_auth_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Ensure clean test state before starting
DELETE FROM users WHERE id IN (101, 102);
```

### 1.2 Web Application Precondition
1. Deploy `Servlet_SD` to Apache Tomcat (port `8080`).
2. Open Chrome/Firefox and navigate to:  
   `http://localhost:8080/Servlet_SD/` (or `http://localhost:8080/index.html`)
3. Open **Browser Developer Tools** (`F12`) -> **Network Tab** to monitor HTTP POST requests and payloads.

---

## 2. Test Execution Matrix

| Test ID | Action Under Test | Test Scenario | Expected HTTP Status | Expected UI Output | Expected DB State | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | `Add` (INSERT) | Add new valid user with ID `101` | `200 OK` | "User Added Successfully" | 1 row inserted | [ ] Pass |
| **TC-02** | `Add` (Negative) | Attempt to add duplicate ID `101` | `200 OK` | "Database Conflict" | No new row | [ ] Pass |
| **TC-03** | `View` (SELECT) | Query existing User ID `101` | `200 OK` | Displays ID, Name, Email | Record read only | [ ] Pass |
| **TC-04** | `View` (Negative) | Query non-existing User ID `999` | `200 OK` | "No user exists with ID: 999" | No change | [ ] Pass |
| **TC-05** | `Update` (UPDATE) | Update Name & Email for ID `101` | `200 OK` | "User Updated Successfully" | Row updated | [ ] Pass |
| **TC-06** | `Delete` (DELETE) | Delete existing User ID `101` | `200 OK` | "User Deleted Successfully" | 0 rows for ID 101 | [ ] Pass |
| **TC-07** | Boundary / Security | Test SQL Injection `' OR '1'='1` | `200 OK` | Input error / safe escaping | DB protected | [ ] Pass |

---

## 3. Step-by-Step Test Execution Instructions

### Test Case 1: Add User (Create - INSERT)
* **Objective:** Verify that submitting the form with action `Add` executes an `INSERT` statement and persists the record to MySQL.
* **Steps:**
  1. Navigate to the form (`index.html`).
  2. In **User ID**, enter `101`.
  3. In **Full Name**, enter `Alice Johnson`.
  4. In **Email Address**, enter `alice.johnson@example.com`.
  5. Click the **➕ Add** button.
* **UI Verification:**
  - The browser is directed to `/UserServlet`.
  - A green badge displaying `SUCCESS` appears.
  - Heading: `User Added Successfully`.
  - Details table lists `User ID: 101`, `Full Name: Alice Johnson`, `Email: alice.johnson@example.com`.
* **Database Verification (MySQL Console):**
  ```sql
  SELECT id, name, email FROM users WHERE id = 101;
  ```
  **Expected DB Output:**
  ```text
  +-----+---------------+---------------------------+
  | id  | name          | email                     |
  +-----+---------------+---------------------------+
  | 101 | Alice Johnson | alice.johnson@example.com |
  +-----+---------------+---------------------------+
  1 row in set
  ```

---

### Test Case 2: View User (Read - SELECT)
* **Objective:** Verify that clicking `View` retrieves the exact matching record using `SELECT id, name, email FROM users WHERE id = ?`.
* **Steps:**
  1. Click **Return to Form**.
  2. In **User ID**, enter `101`.
  3. Leave **Full Name** and **Email Address** empty.
  4. Click the **🔍 View** button.
* **UI Verification:**
  - Heading displays `User Found`.
  - Message displays: `Record retrieved successfully.`
  - Table accurately displays:
    - User ID: `101`
    - Full Name: `Alice Johnson`
    - Email: `alice.johnson@example.com`
* **Database Verification:**
  Confirm no rows were altered (`updated_at` unchanged).

---

### Test Case 3: Update User (Update - UPDATE)
* **Objective:** Verify that clicking `Update` executes `UPDATE users SET name = ?, email = ? WHERE id = ?` and updates the record.
* **Steps:**
  1. Click **Return to Form**.
  2. In **User ID**, enter `101`.
  3. In **Full Name**, modify to `Alice J. Smith`.
  4. In **Email Address**, modify to `alice.smith@example.com`.
  5. Click the **✏️ Update** button.
* **UI Verification:**
  - Heading displays `User Updated Successfully`.
  - Message: `User record updated in MySQL database.`
  - Displayed table reflects the updated details.
* **Database Verification (MySQL Console):**
  ```sql
  SELECT id, name, email FROM users WHERE id = 101;
  ```
  **Expected DB Output:**
  ```text
  +-----+----------------+-------------------------+
  | id  | name           | email                   |
  +-----+----------------+-------------------------+
  | 101 | Alice J. Smith | alice.smith@example.com |
  +-----+----------------+-------------------------+
  ```

---

### Test Case 4: Delete User (Delete - DELETE)
* **Objective:** Verify that clicking `Delete` invokes `DELETE FROM users WHERE id = ?` and purges the row from MySQL.
* **Steps:**
  1. Click **Return to Form**.
  2. In **User ID**, enter `101`.
  3. Click the **🗑️ Delete** button.
  4. An alert prompt appears: *"Are you sure you want to permanently delete User ID: 101?"* Click **OK**.
* **UI Verification:**
  - Heading displays `User Deleted Successfully`.
  - Message: `User ID 101 was permanently removed from database.`
* **Database Verification (MySQL Console):**
  ```sql
  SELECT * FROM users WHERE id = 101;
  ```
  **Expected DB Output:**
  ```text
  Empty set (0.00 sec)
  ```

---

### Test Case 5: Negative & Validation Testing
1. **Empty User ID on View/Delete:**
   - Leave ID empty and click **View**.
   - Expected: Red error card: `"User ID is required to view a record."`
2. **Non-Numeric ID input:**
   - In ID field, enter non-number characters (or inspect element to inject `abc`).
   - Expected: Catch `NumberFormatException` returning: `"User ID must be a valid integer."`
3. **SQL Injection Resilience:**
   - Enter `101` in ID and `' OR '1'='1` in Name.
   - Expected: `PreparedStatement` safely parametrizes the payload as literal string value without syntax corruption or unauthorized table exposure.

---

## 4. QA Sign-Off Checklist

- [ ] All 4 CRUD buttons trigger corresponding HTTP POST actions to `/UserServlet`.
- [ ] PreparedStatements used for all database queries (zero SQL injection vulnerabilities).
- [ ] Database state strictly matches UI feedback on all actions.
- [ ] Clean error handling for duplicates, missing fields, and non-existent IDs.
