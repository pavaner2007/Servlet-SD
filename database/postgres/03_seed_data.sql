-- ============================================================================
-- Seed Data for Library Management System (PostgreSQL)
-- ============================================================================

-- Connect to user_auth_db first
-- Insert Users
INSERT INTO users (user_id, username, password_hash, email, first_name, last_name, role, status) VALUES
(1, 'admin', '$2a$12$e8wS97oD23m8x0Vp/Rk0he.qS74X7uF9x/0z0tZ0O4W12j0h0g0qO', 'admin@library.org', 'System', 'Admin', 'ADMIN', 'ACTIVE'),
(2, 'librarian_jane', '$2a$12$e8wS97oD23m8x0Vp/Rk0he.qS74X7uF9x/0z0tZ0O4W12j0h0g0qO', 'jane.doe@library.org', 'Jane', 'Doe', 'LIBRARIAN', 'ACTIVE'),
(3, 'member_alice', '$2a$12$e8wS97oD23m8x0Vp/Rk0he.qS74X7uF9x/0z0tZ0O4W12j0h0g0qO', 'alice.smith@gmail.com', 'Alice', 'Smith', 'MEMBER', 'ACTIVE'),
(4, 'member_bob', '$2a$12$e8wS97oD23m8x0Vp/Rk0he.qS74X7uF9x/0z0tZ0O4W12j0h0g0qO', 'bob.johnson@gmail.com', 'Bob', 'Johnson', 'MEMBER', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

-- Connect to library_db
-- Insert Members
INSERT INTO members (member_id, user_id, membership_no, first_name, last_name, email, phone, membership_status, membership_tier, max_active_loans, current_loan_count, expiry_date) VALUES
(1, 3, 'MEM-2026-0001', 'Alice', 'Smith', 'alice.smith@gmail.com', '+1-555-0101', 'ACTIVE', 'STANDARD', 5, 2, '2027-12-31'),
(2, 4, 'MEM-2026-0002', 'Bob', 'Johnson', 'bob.johnson@gmail.com', '+1-555-0102', 'ACTIVE', 'STUDENT', 3, 1, '2027-06-30')
ON CONFLICT (member_id) DO NOTHING;

-- Insert Book Titles
INSERT INTO book_titles (book_id, isbn, isbn13, title, subtitle, authors, publisher, publication_year, edition, language, genre, category, description, total_physical_copies, available_physical_copies, total_digital_licences, available_digital_licences) VALUES
(1, '0134685997', '978-0134685991', 'Effective Java', 'Best practices for the Java platform', 'Joshua Bloch', 'Addison-Wesley', 2018, '3rd Edition', 'English', 'Computer Science', 'Programming', 'The definitive guide to Java best practices and design patterns.', 4, 1, 5, 4),
(2, '0132350882', '978-0132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship', 'Robert C. Martin', 'Prentice Hall', 2008, '1st Edition', 'English', 'Software Engineering', 'Programming', 'Even bad code can function. But if code isn’t clean, it can bring a development organization to its knees.', 3, 2, 2, 2),
(3, '0596007124', '978-0596007126', 'Head First Design Patterns', 'Building Extensible and Maintainable Object-Oriented Software', 'Eric Freeman, Elisabeth Robson', 'O Reilly Media', 2020, '2nd Edition', 'English', 'Software Architecture', 'Design Patterns', 'A brain-friendly guide to object-oriented design principles and patterns.', 2, 0, 3, 1)
ON CONFLICT (book_id) DO NOTHING;

-- Insert Physical Copies (status: AVAILABLE, LOANED, RESERVED, LOST)
INSERT INTO physical_copies (copy_id, book_id, barcode, rfid_tag, shelf_location, condition_note, status, acquired_date, price) VALUES
(1, 1, 'BC-EJ-001', 'RFID-EJ-001', 'Floor 2, Shelf CS-12', 'MINT', 'AVAILABLE', '2024-01-15', 49.99),
(2, 1, 'BC-EJ-002', 'RFID-EJ-002', 'Floor 2, Shelf CS-12', 'GOOD', 'LOANED', '2024-01-15', 49.99),
(3, 1, 'BC-EJ-003', 'RFID-EJ-003', 'Floor 2, Shelf CS-12', 'GOOD', 'RESERVED', '2024-02-10', 49.99),
(4, 1, 'BC-EJ-004', 'RFID-EJ-004', 'Floor 2, Shelf CS-12', 'FAIR', 'LOST', '2024-02-10', 49.99),
(5, 2, 'BC-CC-001', 'RFID-CC-001', 'Floor 2, Shelf CS-14', 'MINT', 'AVAILABLE', '2024-03-01', 44.50),
(6, 2, 'BC-CC-002', 'RFID-CC-002', 'Floor 2, Shelf CS-14', 'GOOD', 'AVAILABLE', '2024-03-01', 44.50),
(7, 2, 'BC-CC-003', 'RFID-CC-003', 'Floor 2, Shelf CS-14', 'FAIR', 'LOANED', '2024-03-01', 44.50),
(8, 3, 'BC-DP-001', 'RFID-DP-001', 'Floor 2, Shelf CS-16', 'GOOD', 'LOANED', '2024-04-12', 55.00),
(9, 3, 'BC-DP-002', 'RFID-DP-002', 'Floor 2, Shelf CS-16', 'MINT', 'RESERVED', '2024-04-12', 55.00)
ON CONFLICT (copy_id) DO NOTHING;

-- Insert Digital Licences
INSERT INTO digital_licences (licence_id, book_id, licence_key, provider, format, valid_from, valid_to, max_concurrent_checkouts, active_checkouts, is_active) VALUES
(1, 1, 'LIC-EJ-OD-2026', 'OverDrive', 'EPUB', '2026-01-01', '2027-01-01', 5, 1, TRUE),
(2, 2, 'LIC-CC-PQ-2026', 'ProQuest', 'PDF', '2026-01-01', '2027-01-01', 2, 0, TRUE),
(3, 3, 'LIC-DP-OD-2026', 'OverDrive', 'EPUB', '2026-01-01', '2027-01-01', 3, 2, TRUE)
ON CONFLICT (licence_id) DO NOTHING;

-- Insert Active & Historical Loans
INSERT INTO loans (loan_id, member_id, loan_type, copy_id, licence_id, checkout_date, due_date, returned_date, status, renewal_count, fine_amount, issued_by_user_id) VALUES
(1, 1, 'PHYSICAL', 2, NULL, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '9 days', NULL, 'ACTIVE', 0, 0.00, 2),
(2, 1, 'DIGITAL', NULL, 1, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '12 days', NULL, 'ACTIVE', 0, 0.00, NULL),
(3, 2, 'PHYSICAL', 7, NULL, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '6 days', NULL, 'OVERDUE', 1, 3.50, 2)
ON CONFLICT (loan_id) DO NOTHING;

-- Insert Reservations
INSERT INTO reservations (reservation_id, member_id, book_id, allocated_copy_id, reservation_date, queue_position, expiry_date, status) VALUES
(1, 2, 1, 3, CURRENT_TIMESTAMP - INTERVAL '1 day', 1, CURRENT_TIMESTAMP + INTERVAL '2 days', 'PENDING'),
(2, 1, 3, 9, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP + INTERVAL '3 days', 'PENDING')
ON CONFLICT (reservation_id) DO NOTHING;
