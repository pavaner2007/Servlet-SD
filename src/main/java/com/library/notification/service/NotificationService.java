package com.library.notification.service;

import com.library.config.DatabaseConnectionManager;
import com.library.messaging.model.BookReturnedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/**
 * Notification Service:
 * Consumes BookReturnedEvents, alerts the next member in the waitlist queue,
 * and handles overdue due-date reminders via Email & SMS logging.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final String FIND_NEXT_RESERVATION = 
        "SELECT r.reservation_id, r.member_id, r.queue_position, r.expiry_date, " +
        "m.first_name, m.last_name, m.email, m.phone, b.title " +
        "FROM reservations r " +
        "JOIN members m ON r.member_id = m.member_id " +
        "JOIN book_titles b ON r.book_id = b.book_id " +
        "WHERE r.book_id = ? AND r.status = 'PENDING' " +
        "ORDER BY r.queue_position ASC LIMIT 1";

    private static final String UPDATE_RESERVATION_ALLOCATION = 
        "UPDATE reservations SET allocated_copy_id = ?, notified_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
        "WHERE reservation_id = ?";

    private static final String INSERT_NOTIFICATION_LOG = 
        "INSERT INTO notification_logs (member_id, channel, recipient, subject, message_body, trigger_event, dispatch_status) " +
        "VALUES (?, ?, ?, ?, ?, ?, 'SENT')";

    /**
     * Consumes BookReturnedEvent and alerts the next member in the reservation queue.
     */
    public boolean processBookReturned(BookReturnedEvent event) {
        logger.info("[EVENT CONSUMED] Processing BookReturnedEvent for Book ID: {}, Copy: {}", 
                event.getBookId(), event.getBarcode());

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
            long reservationId = 0;
            long memberId = 0;
            String memberName = "";
            String email = "";
            String phone = "";
            String bookTitle = "";

            try (PreparedStatement ps = conn.prepareStatement(FIND_NEXT_RESERVATION)) {
                ps.setLong(1, event.getBookId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reservationId = rs.getLong("reservation_id");
                        memberId = rs.getLong("member_id");
                        memberName = rs.getString("first_name") + " " + rs.getString("last_name");
                        email = rs.getString("email");
                        phone = rs.getString("phone");
                        bookTitle = rs.getString("title");
                    } else {
                        logger.info("No pending reservations in queue for Book ID: {}. Inventory is available for general loans.", event.getBookId());
                        return false;
                    }
                }
            }

            // Allocate copy to reservation
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_RESERVATION_ALLOCATION)) {
                ps.setLong(1, event.getCopyId());
                ps.setLong(2, reservationId);
                ps.executeUpdate();
            }

            event.setAllocatedReservationId(reservationId);
            event.setNextMemberId(memberId);

            // Construct Alert Messages
            String emailSubject = "Library Alert: Your reserved book '" + bookTitle + "' is ready for pickup!";
            String emailBody = String.format(
                "Hello %s,\n\nA physical copy (Barcode: %s) of your reserved book '%s' has just been returned. " +
                "It is held at the circulation desk under Reservation #%d. Please collect it within 48 hours.\n\nHappy Reading,\nLibrary Circulation Team",
                memberName, event.getBarcode(), bookTitle, reservationId
            );

            String smsBody = String.format("Library Alert: '%s' is now available! Held for %s (Res #%d). Pickup within 48h.",
                    bookTitle, memberName, reservationId);

            // Dispatch & Log Email
            logNotification(conn, memberId, "EMAIL", email, emailSubject, emailBody, "BOOK_RETURNED_ALLOCATION");
            logger.info("📧 [EMAIL DISPATCHED] To: {} | Subject: {}", email, emailSubject);

            // Dispatch & Log SMS
            if (phone != null && !phone.trim().isEmpty()) {
                logNotification(conn, memberId, "SMS", phone, "SMS Alert", smsBody, "BOOK_RETURNED_ALLOCATION");
                logger.info("📱 [SMS DISPATCHED] To: {} | Message: {}", phone, smsBody);
            }

            return true;

        } catch (SQLException e) {
            logger.error("Error processing BookReturnedEvent for event ID: {}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * Dispatches an overdue reminder notice for a loan.
     */
    public void dispatchOverdueNotice(long memberId, String memberName, String email, String phone,
                                     String bookTitle, String barcode, LocalDate dueDate, long daysOverdue, BigDecimal fine) {
        try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
            String subject = "URGENT: Overdue Library Book Reminder - " + bookTitle;
            String emailBody = String.format(
                "Dear %s,\n\nOur records indicate that '%s' (Barcode: %s) was due on %s and is currently %d day(s) overdue.\n" +
                "Accrued fine to date: $%s. Please return this item promptly to avoid suspension.\n\nLibrary Management",
                memberName, bookTitle, barcode, dueDate, daysOverdue, fine.toPlainString()
            );

            logNotification(conn, memberId, "EMAIL", email, subject, emailBody, "OVERDUE_REMINDER");
            logger.warn("🚨 [OVERDUE EMAIL] Dispatched to {} for item '{}' (Due: {}, Fine: ${})", 
                    email, bookTitle, dueDate, fine);

            if (phone != null && !phone.trim().isEmpty()) {
                String smsBody = String.format("Overdue Notice: '%s' is %d days overdue (Due: %s). Accrued fine: $%s. Return ASAP.",
                        bookTitle, daysOverdue, dueDate, fine);
                logNotification(conn, memberId, "SMS", phone, "Overdue SMS", smsBody, "OVERDUE_REMINDER");
                logger.warn("🚨 [OVERDUE SMS] Dispatched to {}", phone);
            }

        } catch (SQLException e) {
            logger.error("Failed to log overdue notice for member: {}", memberId, e);
        }
    }

    private void logNotification(Connection conn, long memberId, String channel, String recipient,
                                 String subject, String body, String eventType) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_NOTIFICATION_LOG)) {
            ps.setLong(1, memberId);
            ps.setString(2, channel);
            ps.setString(3, recipient);
            ps.setString(4, subject);
            ps.setString(5, body);
            ps.setString(6, eventType);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert notification_logs entry for member: {}", memberId, e);
        }
    }
}
