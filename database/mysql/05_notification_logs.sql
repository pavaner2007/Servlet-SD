-- ============================================================================
-- Notification Service - Audit & Dispatch Log Schema
-- Database: library_db
-- ============================================================================

CREATE TABLE IF NOT EXISTS notification_logs (
    notification_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    channel             ENUM('EMAIL', 'SMS', 'IN_APP') NOT NULL DEFAULT 'EMAIL',
    recipient           VARCHAR(255) NOT NULL,
    subject             VARCHAR(255) NOT NULL,
    message_body        TEXT NOT NULL,
    trigger_event       VARCHAR(100) NOT NULL, -- e.g. "BOOK_RETURNED_ALLOCATION", "OVERDUE_REMINDER"
    dispatch_status     ENUM('SENT', 'FAILED', 'QUEUED') NOT NULL DEFAULT 'SENT',
    dispatched_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_member (member_id),
    INDEX idx_notif_event (trigger_event),
    INDEX idx_notif_date (dispatched_at)
) ENGINE=InnoDB;
