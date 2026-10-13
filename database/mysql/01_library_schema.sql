-- ============================================================================
-- Library Management System - MySQL Relational Schema (8.0+)
-- Database: library_db
-- ============================================================================

CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_db;

-- ----------------------------------------------------------------------------
-- Table: members
-- Linked to user_id in user_auth_db
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS members (
    member_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    membership_no       VARCHAR(32) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    phone               VARCHAR(30),
    membership_status   ENUM('ACTIVE', 'SUSPENDED', 'EXPIRED', 'PENDING') NOT NULL DEFAULT 'ACTIVE',
    membership_tier     ENUM('STANDARD', 'STUDENT', 'FACULTY', 'PREMIUM') NOT NULL DEFAULT 'STANDARD',
    max_active_loans    INT NOT NULL DEFAULT 5,
    current_loan_count  INT NOT NULL DEFAULT 0,
    joined_date         DATE NOT NULL,
    expiry_date         DATE NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_members_email (email),
    INDEX idx_members_status (membership_status),
    INDEX idx_members_user_id (user_id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: book_titles
-- Bibliographic records
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book_titles (
    book_id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn                        VARCHAR(20) NOT NULL UNIQUE,
    isbn13                      VARCHAR(20) UNIQUE,
    title                       VARCHAR(255) NOT NULL,
    subtitle                    VARCHAR(255),
    authors                     TEXT NOT NULL,
    publisher                   VARCHAR(150),
    publication_year            INT,
    edition                     VARCHAR(50),
    language                    VARCHAR(50) DEFAULT 'English',
    genre                       VARCHAR(100),
    category                    VARCHAR(100),
    classification_code         VARCHAR(50),
    description                 TEXT,
    total_physical_copies       INT NOT NULL DEFAULT 0,
    available_physical_copies   INT NOT NULL DEFAULT 0,
    total_digital_licences      INT NOT NULL DEFAULT 0,
    available_digital_licences  INT NOT NULL DEFAULT 0,
    cover_image_url             VARCHAR(500),
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_titles_isbn (isbn),
    INDEX idx_book_titles_title (title),
    INDEX idx_book_titles_genre (genre),
    INDEX idx_book_titles_category (category)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: physical_copies
-- Status enum constraint: AVAILABLE, LOANED, RESERVED, LOST
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS physical_copies (
    copy_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id             BIGINT NOT NULL,
    barcode             VARCHAR(64) NOT NULL UNIQUE,
    rfid_tag            VARCHAR(64) UNIQUE,
    shelf_location      VARCHAR(100) NOT NULL,
    condition_note      VARCHAR(255) DEFAULT 'GOOD',
    status              ENUM('AVAILABLE', 'LOANED', 'RESERVED', 'LOST') NOT NULL DEFAULT 'AVAILABLE',
    acquired_date       DATE NOT NULL,
    price               DECIMAL(10, 2),
    version             INT NOT NULL DEFAULT 1,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_physical_copies_status (status),
    INDEX idx_physical_copies_barcode (barcode),
    CONSTRAINT fk_physical_copies_book FOREIGN KEY (book_id) 
        REFERENCES book_titles (book_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: digital_licences
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS digital_licences (
    licence_id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id                     BIGINT NOT NULL,
    licence_key                 VARCHAR(128) NOT NULL UNIQUE,
    provider                    VARCHAR(100) NOT NULL,
    format                      VARCHAR(30) NOT NULL DEFAULT 'EPUB',
    valid_from                  DATETIME NOT NULL,
    valid_to                    DATETIME,
    max_concurrent_checkouts    INT NOT NULL DEFAULT 1,
    active_checkouts            INT NOT NULL DEFAULT 0,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_digital_licences_active (is_active),
    CONSTRAINT fk_digital_licences_book FOREIGN KEY (book_id) 
        REFERENCES book_titles (book_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: loans
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS loans (
    loan_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    loan_type           ENUM('PHYSICAL', 'DIGITAL') NOT NULL,
    copy_id             BIGINT NULL,
    licence_id          BIGINT NULL,
    checkout_date       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date            DATETIME NOT NULL,
    returned_date       DATETIME NULL,
    status              ENUM('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST') NOT NULL DEFAULT 'ACTIVE',
    renewal_count       INT NOT NULL DEFAULT 0,
    max_renewals        INT NOT NULL DEFAULT 2,
    fine_amount         DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    fine_paid           BOOLEAN NOT NULL DEFAULT FALSE,
    issued_by_user_id   BIGINT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loans_member_id (member_id),
    INDEX idx_loans_status (status),
    INDEX idx_loans_due_date (due_date),
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id) 
        REFERENCES members (member_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_copy FOREIGN KEY (copy_id) 
        REFERENCES physical_copies (copy_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_licence FOREIGN KEY (licence_id) 
        REFERENCES digital_licences (licence_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: reservations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    book_id             BIGINT NOT NULL,
    allocated_copy_id   BIGINT NULL,
    reservation_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    queue_position      INT NOT NULL DEFAULT 1,
    expiry_date         DATETIME NOT NULL,
    notified_at         DATETIME NULL,
    status              ENUM('PENDING', 'FULFILLED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reservations_member (member_id),
    INDEX idx_reservations_book (book_id),
    INDEX idx_reservations_status (status),
    CONSTRAINT fk_reservations_member FOREIGN KEY (member_id) 
        REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_book FOREIGN KEY (book_id) 
        REFERENCES book_titles (book_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_copy FOREIGN KEY (allocated_copy_id) 
        REFERENCES physical_copies (copy_id) ON DELETE SET NULL
) ENGINE=InnoDB;
