-- ============================================================================
-- Library Management System - PostgreSQL Relational Schema
-- Database: library_db
-- ============================================================================

-- Create Enums
CREATE TYPE copy_status_enum AS ENUM ('AVAILABLE', 'LOANED', 'RESERVED', 'LOST');
CREATE TYPE loan_status_enum AS ENUM ('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST');
CREATE TYPE loan_type_enum AS ENUM ('PHYSICAL', 'DIGITAL');
CREATE TYPE reservation_status_enum AS ENUM ('PENDING', 'FULFILLED', 'CANCELLED', 'EXPIRED');
CREATE TYPE membership_status_enum AS ENUM ('ACTIVE', 'SUSPENDED', 'EXPIRED', 'PENDING');
CREATE TYPE membership_tier_enum AS ENUM ('STANDARD', 'STUDENT', 'FACULTY', 'PREMIUM');

-- ----------------------------------------------------------------------------
-- Table: Members
-- Interconnects with Member profiles, loans, and reservations.
-- user_id references user entity in the separate User Management module.
-- ----------------------------------------------------------------------------
CREATE TABLE members (
    member_id           BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE, -- Linked to User Auth DB user_id
    membership_no       VARCHAR(32) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    phone               VARCHAR(30),
    membership_status   membership_status_enum NOT NULL DEFAULT 'ACTIVE',
    membership_tier     membership_tier_enum NOT NULL DEFAULT 'STANDARD',
    max_active_loans    INT NOT NULL DEFAULT 5 CHECK (max_active_loans >= 0),
    current_loan_count  INT NOT NULL DEFAULT 0 CHECK (current_loan_count >= 0),
    joined_date         DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date         DATE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_members_email ON members(email);
CREATE INDEX idx_members_status ON members(membership_status);
CREATE INDEX idx_members_user_id ON members(user_id);

-- ----------------------------------------------------------------------------
-- Table: BookTitles
-- Canonical catalogue record representing bibliographic works.
-- ----------------------------------------------------------------------------
CREATE TABLE book_titles (
    book_id                     BIGSERIAL PRIMARY KEY,
    isbn                        VARCHAR(20) NOT NULL UNIQUE,
    isbn13                      VARCHAR(20) UNIQUE,
    title                       VARCHAR(255) NOT NULL,
    subtitle                    VARCHAR(255),
    authors                     TEXT NOT NULL, -- Comma-separated or serialized authors
    publisher                   VARCHAR(150),
    publication_year            INT CHECK (publication_year > 1000 AND publication_year <= EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    edition                     VARCHAR(50),
    language                    VARCHAR(50) DEFAULT 'English',
    genre                       VARCHAR(100),
    category                    VARCHAR(100),
    classification_code         VARCHAR(50), -- e.g. Dewey Decimal / LoC
    description                 TEXT,
    total_physical_copies       INT NOT NULL DEFAULT 0 CHECK (total_physical_copies >= 0),
    available_physical_copies   INT NOT NULL DEFAULT 0 CHECK (available_physical_copies >= 0),
    total_digital_licences      INT NOT NULL DEFAULT 0 CHECK (total_digital_licences >= 0),
    available_digital_licences  INT NOT NULL DEFAULT 0 CHECK (available_digital_licences >= 0),
    cover_image_url             VARCHAR(500),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_avail_phys_lte_total CHECK (available_physical_copies <= total_physical_copies),
    CONSTRAINT chk_avail_digi_lte_total CHECK (available_digital_licences <= total_digital_licences)
);

CREATE INDEX idx_book_titles_isbn ON book_titles(isbn);
CREATE INDEX idx_book_titles_title ON book_titles(title);
CREATE INDEX idx_book_titles_genre ON book_titles(genre);
CREATE INDEX idx_book_titles_category ON book_titles(category);

-- ----------------------------------------------------------------------------
-- Table: PhysicalCopies
-- Inventory item instances for each physical book copy.
-- Status constraint: AVAILABLE, LOANED, RESERVED, LOST
-- ----------------------------------------------------------------------------
CREATE TABLE physical_copies (
    copy_id             BIGSERIAL PRIMARY KEY,
    book_id             BIGINT NOT NULL REFERENCES book_titles(book_id) ON DELETE CASCADE,
    barcode             VARCHAR(64) NOT NULL UNIQUE,
    rfid_tag            VARCHAR(64) UNIQUE,
    shelf_location      VARCHAR(100) NOT NULL, -- e.g. "Section B, Shelf 4, Row 2"
    condition_note      VARCHAR(255) DEFAULT 'GOOD', -- 'MINT', 'GOOD', 'FAIR', 'DAMAGED'
    status              copy_status_enum NOT NULL DEFAULT 'AVAILABLE',
    acquired_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    price               NUMERIC(10, 2) CHECK (price >= 0),
    version             INT NOT NULL DEFAULT 1,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_physical_copies_book_id ON physical_copies(book_id);
CREATE INDEX idx_physical_copies_barcode ON physical_copies(barcode);
CREATE INDEX idx_physical_copies_status ON physical_copies(status);

-- ----------------------------------------------------------------------------
-- Table: DigitalLicences
-- Digital rights management for eBooks/Audiobooks per book title.
-- ----------------------------------------------------------------------------
CREATE TABLE digital_licences (
    licence_id                  BIGSERIAL PRIMARY KEY,
    book_id                     BIGINT NOT NULL REFERENCES book_titles(book_id) ON DELETE CASCADE,
    licence_key                 VARCHAR(128) NOT NULL UNIQUE,
    provider                    VARCHAR(100) NOT NULL, -- e.g. "OverDrive", "ProQuest", "Internal"
    format                      VARCHAR(30) NOT NULL DEFAULT 'EPUB', -- 'EPUB', 'PDF', 'AUDIOBOOK'
    valid_from                  TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to                    TIMESTAMP WITH TIME ZONE,
    max_concurrent_checkouts    INT NOT NULL DEFAULT 1 CHECK (max_concurrent_checkouts > 0),
    active_checkouts            INT NOT NULL DEFAULT 0 CHECK (active_checkouts >= 0),
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_licence_checkouts CHECK (active_checkouts <= max_concurrent_checkouts)
);

CREATE INDEX idx_digital_licences_book_id ON digital_licences(book_id);
CREATE INDEX idx_digital_licences_active ON digital_licences(is_active);

-- ----------------------------------------------------------------------------
-- Table: Loans
-- Tracks physical and digital borrowing lifecycle.
-- ----------------------------------------------------------------------------
CREATE TABLE loans (
    loan_id             BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL REFERENCES members(member_id) ON DELETE RESTRICT,
    loan_type           loan_type_enum NOT NULL,
    copy_id             BIGINT REFERENCES physical_copies(copy_id) ON DELETE RESTRICT,
    licence_id          BIGINT REFERENCES digital_licences(licence_id) ON DELETE RESTRICT,
    checkout_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date            TIMESTAMP WITH TIME ZONE NOT NULL,
    returned_date       TIMESTAMP WITH TIME ZONE,
    status              loan_status_enum NOT NULL DEFAULT 'ACTIVE',
    renewal_count       INT NOT NULL DEFAULT 0 CHECK (renewal_count >= 0),
    max_renewals        INT NOT NULL DEFAULT 2,
    fine_amount         NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (fine_amount >= 0),
    fine_paid           BOOLEAN NOT NULL DEFAULT FALSE,
    issued_by_user_id   BIGINT, -- Staff/Librarian ID from User Auth DB
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Polymorphic consistency check
    CONSTRAINT chk_loan_target CHECK (
        (loan_type = 'PHYSICAL' AND copy_id IS NOT NULL AND licence_id IS NULL) OR
        (loan_type = 'DIGITAL' AND licence_id IS NOT NULL AND copy_id IS NULL)
    )
);

CREATE INDEX idx_loans_member_id ON loans(member_id);
CREATE INDEX idx_loans_copy_id ON loans(copy_id);
CREATE INDEX idx_loans_licence_id ON loans(licence_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_due_date ON loans(due_date);

-- ----------------------------------------------------------------------------
-- Table: Reservations
-- Queued waitlist for BookTitles when copies/licences are checked out.
-- ----------------------------------------------------------------------------
CREATE TABLE reservations (
    reservation_id      BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL REFERENCES members(member_id) ON DELETE CASCADE,
    book_id             BIGINT NOT NULL REFERENCES book_titles(book_id) ON DELETE CASCADE,
    allocated_copy_id   BIGINT REFERENCES physical_copies(copy_id) ON DELETE SET NULL,
    reservation_date    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    queue_position      INT NOT NULL DEFAULT 1 CHECK (queue_position > 0),
    expiry_date         TIMESTAMP WITH TIME ZONE NOT NULL,
    notified_at         TIMESTAMP WITH TIME ZONE,
    status              reservation_status_enum NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_member_book_pending UNIQUE (member_id, book_id, status)
);

CREATE INDEX idx_reservations_member_id ON reservations(member_id);
CREATE INDEX idx_reservations_book_id ON reservations(book_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_queue ON reservations(book_id, queue_position);
