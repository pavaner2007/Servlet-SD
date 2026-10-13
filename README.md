# Library Management System

An enterprise-grade, high-performance Library Management System built with relational databases (PostgreSQL/MySQL), dual database architecture, and Elasticsearch for sub-millisecond catalogue discovery.

---

## Architecture Overview

```
                      +-----------------------------+
                      |   Library Management App    |
                      +--------------+--------------+
                                     |
         +---------------------------+---------------------------+
         |                           |                           |
         v                           v                           v
+------------------+       +------------------+       +---------------------+
|    Library DB    |       |   User Auth DB   |       |    Elasticsearch    |
| (Primary Pool)   |       | (Secondary Pool) |       |  (Search Catalogue) |
+------------------+       +------------------+       +---------------------+
| • Members        |       | • Users          |       | • Multi-match Query |
| • BookTitles     |       | • User Audit Log |       | • Edge n-gram Auto- |
| • PhysicalCopies |       | • Role/Security  |       |   complete          |
| • DigitalLicences|       +------------------+       | • Faceted Aggs      |
| • Loans          |                                  +---------------------+
| • Reservations   |
+------------------+
```

---

## 1. Relational Database Schemas

Scripts are provided in `database/` for both **PostgreSQL** and **MySQL (8.0+)**:

| Table | Purpose | Interconnections & Key Rules |
| :--- | :--- | :--- |
| **`members`** | Registered library patrons | Links to `user_id` from the User Auth DB. Tracks `max_active_loans` and `current_loan_count`. |
| **`book_titles`** | Bibliographic work master catalog | Parent of physical copies and digital licences. Tracks total and available stock. |
| **`physical_copies`** | Physical inventory units | Foreign key to `book_titles`. Strict enum status constraint: `AVAILABLE`, `LOANED`, `RESERVED`, `LOST`. |
| **`digital_licences`** | E-book/audiobook licence pool | Foreign key to `book_titles`. Tracks `max_concurrent_checkouts` and `active_checkouts`. |
| **`loans`** | Checkout records | Polymorphic checkout: links to `members` and either `physical_copies` or `digital_licences`. |
| **`reservations`** | Waitlist hold queue | Links `members` and `book_titles`, with FIFO queue position and status tracking. |

### Schema Locations
- **PostgreSQL**:
  - `database/postgres/01_library_schema.sql`
  - `database/postgres/02_users_schema.sql`
  - `database/postgres/03_seed_data.sql`
- **MySQL**:
  - `database/mysql/01_library_schema.sql`
  - `database/mysql/02_users_schema.sql`

---

## 2. Object Models

All Java object models reside under `src/main/java/com/library/model/`:

- `Member.java`: Patron profile with loan entitlement checks.
- `BookTitle.java`: Master catalog record with aggregated availability counts.
- `PhysicalCopy.java`: Physical copy instance with `CopyStatus` (`AVAILABLE`, `LOANED`, `RESERVED`, `LOST`).
- `DigitalLicence.java`: Digital licence entity with concurrency controls.
- `Loan.java`: Checkout lifecycle (renewal, return, overdue fines).
- `Reservation.java`: Waitlist holds and allocation lifecycle.

---

## 3. Dual Database Connection Setup

To isolate sensitive user credentials and security audit records, the system maintains **two isolated connection pools**:

1. **`LibraryDB`**: Catalog, copies, circulation, and reservations.
2. **`UserAuthDB`**: User accounts, passwords, roles, and audit trails.

Configured in `src/main/resources/db.properties` and managed by:
`src/main/java/com/library/config/DatabaseConnectionManager.java`

```java
// Connect to Library DB (Catalog/Circulation)
try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
    // Execute catalog operations
}

// Connect to User Management DB (Auth/Roles/Audit)
try (Connection conn = DatabaseConnectionManager.getUserAuthConnection()) {
    // Execute authentication/user management operations
}
```

---

## 4. Elasticsearch Catalogue Index & Fast Search

Optimized for instant catalog discovery with edge n-gram autocomplete and full-text relevance:

- **Index Settings & Mappings**: `elasticsearch/catalog_index_settings.json`
- **Sample Bulk Index Data**: `elasticsearch/sample_catalog_documents.json`
- **Search Query DSL Templates**: `elasticsearch/search_queries.json`
- **Java Service & Document**: `com.library.search.service.CatalogSearchService`

### Features
1. **Edge n-gram Autocomplete**: Search-as-you-type on `title.autocomplete` and `authors.autocomplete`.
2. **Relevance Boosting**: Weighted scoring across fields (`title^4`, `authors^3`, `subtitle^2`, `description^1`).
3. **Faceted Aggregations**: Real-time facet counts by genre, category, author, and availability.
4. **Normalized ISBN Lookup**: Hyphen- and whitespace-agnostic ISBN-10 / ISBN-13 matching.

---

## Quickstart

### 1. Initialize Relational Databases
**PostgreSQL:**
```bash
psql -U postgres -d library_db -f database/postgres/01_library_schema.sql
psql -U postgres -d user_auth_db -f database/postgres/02_users_schema.sql
psql -U postgres -d library_db -f database/postgres/03_seed_data.sql
```

**MySQL:**
```bash
mysql -u root -p < database/mysql/01_library_schema.sql
mysql -u root -p < database/mysql/02_users_schema.sql
```

### 2. Initialize Elasticsearch Index
```bash
curl -X PUT "localhost:9200/library_catalog" \
     -H "Content-Type: application/json" \
     -d @elasticsearch/catalog_index_settings.json
```

---

## 5. Documentation & Testing Guides

All detailed QA test suites and execution instructions are consolidated in the [`docs/`](docs/) directory:

- [QA_TESTING_SCRIPT.md](docs/QA_TESTING_SCRIPT.md) — Phase 2: User Management Servlet & JDBC CRUD UI test instructions.
- [PHASE3_QA_TESTING_GUIDE.md](docs/PHASE3_QA_TESTING_GUIDE.md) — Phase 3: Circulation REST APIs, Postman collection, and Optimistic Locking race-condition verification.
- [PHASE4_QA_TESTING_GUIDE.md](docs/PHASE4_QA_TESTING_GUIDE.md) — Phase 4: Redis write-through caching, RabbitMQ event messaging, notification logs, and overdue reminder cron job.