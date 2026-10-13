package com.library.cron;

import com.library.config.DatabaseConnectionManager;
import com.library.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled Cron Job:
 * Scans active loans, detects overdue items, computes fines, and dispatches reminder notices.
 * Can be triggered on a recurring schedule or manually on-demand for QA testing.
 */
public class OverdueReminderCronJob {
    private static final Logger logger = LoggerFactory.getLogger(OverdueReminderCronJob.class);

    private static volatile OverdueReminderCronJob instance;
    private final NotificationService notificationService;
    private final ScheduledExecutorService scheduler;

    private static final String SELECT_OVERDUE_LOANS = 
        "SELECT l.loan_id, l.member_id, l.copy_id, l.due_date, l.fine_amount, " +
        "m.first_name, m.last_name, m.email, m.phone, b.title, c.barcode " +
        "FROM loans l " +
        "JOIN members m ON l.member_id = m.member_id " +
        "JOIN physical_copies c ON l.copy_id = c.copy_id " +
        "JOIN book_titles b ON c.book_id = b.book_id " +
        "WHERE l.status IN ('ACTIVE', 'OVERDUE') AND l.due_date < CURRENT_TIMESTAMP";

    private static final String UPDATE_LOAN_FINE = 
        "UPDATE loans SET status = 'OVERDUE', fine_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE loan_id = ?";

    private OverdueReminderCronJob() {
        this.notificationService = new NotificationService();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "overdue-reminder-cron-thread");
            t.setDaemon(true);
            return t;
        });

        // Default schedule: Run every 24 hours, initial delay 1 minute
        this.scheduler.scheduleAtFixedRate(this::executeScan, 1, 24 * 60, TimeUnit.MINUTES);
        logger.info("Overdue reminder cron job registered. Interval: 24h.");
    }

    public static OverdueReminderCronJob getInstance() {
        if (instance == null) {
            synchronized (OverdueReminderCronJob.class) {
                if (instance == null) {
                    instance = new OverdueReminderCronJob();
                }
            }
        }
        return instance;
    }

    /**
     * Executes the overdue scan. Can be invoked manually via QA test endpoints.
     */
    public Map<String, Object> executeScan() {
        logger.info("⏰ [CRON JOB STARTED] Scanning for overdue library loans...");
        int scannedCount = 0;
        int notifiedCount = 0;
        BigDecimal totalFinesCalculated = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_OVERDUE_LOANS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                scannedCount++;
                long loanId = rs.getLong("loan_id");
                long memberId = rs.getLong("member_id");
                Timestamp dueTimestamp = rs.getTimestamp("due_date");
                LocalDate dueDate = dueTimestamp.toLocalDateTime().toLocalDate();
                String memberName = rs.getString("first_name") + " " + rs.getString("last_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String bookTitle = rs.getString("title");
                String barcode = rs.getString("barcode");

                long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                if (daysOverdue <= 0) daysOverdue = 1; // Minimum 1 day overdue

                // Fine rate: $0.50 per overdue day
                BigDecimal fine = BigDecimal.valueOf(daysOverdue * 0.50);
                totalFinesCalculated = totalFinesCalculated.add(fine);

                // Update fine in database
                try (PreparedStatement updatePs = conn.prepareStatement(UPDATE_LOAN_FINE)) {
                    updatePs.setBigDecimal(1, fine);
                    updatePs.setLong(2, loanId);
                    updatePs.executeUpdate();
                }

                // Dispatch Email & SMS notice
                notificationService.dispatchOverdueNotice(
                    memberId, memberName, email, phone, bookTitle, barcode, dueDate, daysOverdue, fine
                );
                notifiedCount++;
            }

            logger.info("✅ [CRON JOB COMPLETED] Scanned: {} overdue loans | Notices Sent: {} | Total Fines: ${}",
                    scannedCount, notifiedCount, totalFinesCalculated);

        } catch (SQLException e) {
            logger.error("Failed to execute overdue reminder cron scan", e);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("status", "SUCCESS");
        summary.put("scannedOverdueLoans", scannedCount);
        summary.put("noticesDispatched", notifiedCount);
        summary.put("totalFinesAccrued", totalFinesCalculated);
        summary.put("executedAt", new java.util.Date());
        return summary;
    }

    public void shutdown() {
        scheduler.shutdown();
        logger.info("Overdue reminder cron job scheduler shutdown.");
    }
}
