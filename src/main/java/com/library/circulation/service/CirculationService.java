package com.library.circulation.service;

import com.library.circulation.exception.OptimisticLockConflictException;
import com.library.config.DatabaseConnectionManager;
import com.library.membership.service.MembershipService;
import com.library.model.Loan;
import com.library.model.LoanStatus;
import com.library.model.LoanType;
import com.library.model.Reservation;
import com.library.model.ReservationStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Circulation Microservice: Manages checkouts, returns, and waitlist reservations.
 * Incorporates Optimistic Locking on physical copy versioning to guarantee concurrency control.
 */
public class CirculationService {

    private final MembershipService membershipService;

    public CirculationService() {
        this.membershipService = new MembershipService();
    }

    public CirculationService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /**
     * Creates a new hold reservation for a member on a book title.
     */
    public Reservation createReservation(Long memberId, Long bookId, int holdDays) throws SQLException {
        // Validate member
        membershipService.validateMemberEligibility(memberId);

        String verifyBookSql = "SELECT book_id FROM book_titles WHERE book_id = ?";
        String nextQueueSql = "SELECT COALESCE(MAX(queue_position), 0) + 1 FROM reservations WHERE book_id = ? AND status = 'PENDING'";
        String insertReservationSql = "INSERT INTO reservations (member_id, book_id, queue_position, expiry_date, status) " +
                                     "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify book exists
                try (PreparedStatement ps = conn.prepareStatement(verifyBookSql)) {
                    ps.setLong(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Book ID " + bookId + " does not exist.");
                        }
                    }
                }

                // Calculate FIFO queue position
                int queuePosition = 1;
                try (PreparedStatement ps = conn.prepareStatement(nextQueueSql)) {
                    ps.setLong(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            queuePosition = rs.getInt(1);
                        }
                    }
                }

                Instant expiryDate = Instant.now().plus(holdDays, ChronoUnit.DAYS);
                long reservationId;

                try (PreparedStatement ps = conn.prepareStatement(insertReservationSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, memberId);
                    ps.setLong(2, bookId);
                    ps.setInt(3, queuePosition);
                    ps.setTimestamp(4, Timestamp.from(expiryDate));
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            reservationId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to obtain generated reservation ID.");
                        }
                    }
                }

                conn.commit();

                Reservation res = new Reservation();
                res.setReservationId(reservationId);
                res.setMemberId(memberId);
                res.setBookId(bookId);
                res.setQueuePosition(queuePosition);
                res.setExpiryDate(expiryDate);
                res.setStatus(ReservationStatus.PENDING);
                res.setCreatedAt(Instant.now());
                return res;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Checks out a physical copy using OPTIMISTIC LOCKING.
     * Prevents competing reservations from checking out the same scarce copy.
     * Throws OptimisticLockConflictException (HTTP 409) if version mismatch or concurrent checkout occurs.
     */
    public Loan checkoutCopy(Long memberId, String barcode, Integer expectedVersion, int durationDays) throws SQLException {
        // 1. Verify Member eligibility
        membershipService.validateMemberEligibility(memberId);

        String fetchCopySql = "SELECT copy_id, book_id, barcode, status, version FROM physical_copies WHERE barcode = ?";
        String optimisticUpdateSql = "UPDATE physical_copies SET status = 'LOANED', version = version + 1, updated_at = CURRENT_TIMESTAMP " +
                                     "WHERE barcode = ? AND status = 'AVAILABLE' AND version = ?";
        String insertLoanSql = "INSERT INTO loans (member_id, loan_type, copy_id, checkout_date, due_date, status, fine_amount) " +
                              "VALUES (?, 'PHYSICAL', ?, ?, ?, 'ACTIVE', 0.00)";
        String decrementBookSql = "UPDATE book_titles SET available_physical_copies = GREATEST(available_physical_copies - 1, 0) WHERE book_id = ?";
        String incrementMemberLoansSql = "UPDATE members SET current_loan_count = current_loan_count + 1 WHERE member_id = ?";
        String fulfillReservationSql = "UPDATE reservations SET status = 'FULFILLED', allocated_copy_id = ?, updated_at = CURRENT_TIMESTAMP " +
                                      "WHERE member_id = ? AND book_id = ? AND status = 'PENDING' LIMIT 1";

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
            conn.setAutoCommit(false);
            try {
                long copyId;
                long bookId;
                String currentStatus;
                int currentVersion;

                // 2. Query current copy state
                try (PreparedStatement ps = conn.prepareStatement(fetchCopySql)) {
                    ps.setString(1, barcode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Physical copy with barcode '" + barcode + "' not found.");
                        }
                        copyId = rs.getLong("copy_id");
                        bookId = rs.getLong("book_id");
                        currentStatus = rs.getString("status");
                        currentVersion = rs.getInt("version");
                    }
                }

                // If expectedVersion not supplied by client, use currentVersion
                int targetVersion = (expectedVersion != null) ? expectedVersion : currentVersion;

                // Check availability precondition
                if (!"AVAILABLE".equalsIgnoreCase(currentStatus)) {
                    throw new OptimisticLockConflictException(
                        "Copy '" + barcode + "' is currently in status " + currentStatus + ", not available for loan.",
                        barcode, targetVersion, currentVersion, currentStatus
                    );
                }

                // 3. ATOMIC OPTIMISTIC LOCK UPDATE
                int rowsUpdated;
                try (PreparedStatement ps = conn.prepareStatement(optimisticUpdateSql)) {
                    ps.setString(1, barcode);
                    ps.setInt(2, targetVersion);
                    rowsUpdated = ps.executeUpdate();
                }

                if (rowsUpdated == 0) {
                    // CONFLICT DETECTED: Another concurrent thread/transaction locked or modified the record first!
                    int latestVersion = currentVersion;
                    String latestStatus = currentStatus;
                    try (PreparedStatement ps = conn.prepareStatement(fetchCopySql)) {
                        ps.setString(1, barcode);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                latestVersion = rs.getInt("version");
                                latestStatus = rs.getString("status");
                            }
                        }
                    }

                    throw new OptimisticLockConflictException(
                        "Optimistic lock conflict: Copy '" + barcode + "' was modified concurrently by another transaction " +
                        "(Attempted version: " + targetVersion + ", Latest version: " + latestVersion + ", Status: " + latestStatus + ").",
                        barcode, targetVersion, latestVersion, latestStatus
                    );
                }

                // 4. Create Loan Record
                Instant checkoutDate = Instant.now();
                Instant dueDate = checkoutDate.plus(durationDays, ChronoUnit.DAYS);
                long loanId;

                try (PreparedStatement ps = conn.prepareStatement(insertLoanSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, memberId);
                    ps.setLong(2, copyId);
                    ps.setTimestamp(3, Timestamp.from(checkoutDate));
                    ps.setTimestamp(4, Timestamp.from(dueDate));
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            loanId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to obtain generated loan ID.");
                        }
                    }
                }

                // 5. Decrement book stock count
                try (PreparedStatement ps = conn.prepareStatement(decrementBookSql)) {
                    ps.setLong(1, bookId);
                    ps.executeUpdate();
                }

                // 6. Increment member active loan count
                try (PreparedStatement ps = conn.prepareStatement(incrementMemberLoansSql)) {
                    ps.setLong(1, memberId);
                    ps.executeUpdate();
                }

                // 7. If this member had a pending reservation on this book, fulfill it
                try (PreparedStatement ps = conn.prepareStatement(fulfillReservationSql)) {
                    ps.setLong(1, copyId);
                    ps.setLong(2, memberId);
                    ps.setLong(3, bookId);
                    ps.executeUpdate();
                }

                conn.commit();

                Loan loan = new Loan();
                loan.setLoanId(loanId);
                loan.setMemberId(memberId);
                loan.setCopyId(copyId);
                loan.setLoanType(LoanType.PHYSICAL);
                loan.setCheckoutDate(checkoutDate);
                loan.setDueDate(dueDate);
                loan.setStatus(LoanStatus.ACTIVE);
                loan.setFineAmount(BigDecimal.ZERO);
                return loan;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Returns a loaned copy back to inventory.
     */
    public Loan returnCopy(Long loanId, String barcode) throws SQLException {
        String findLoanSql = (loanId != null) 
            ? "SELECT l.loan_id, l.member_id, l.copy_id, l.status, c.barcode, c.book_id " +
              "FROM loans l JOIN physical_copies c ON l.copy_id = c.copy_id WHERE l.loan_id = ? AND l.status = 'ACTIVE'"
            : "SELECT l.loan_id, l.member_id, l.copy_id, l.status, c.barcode, c.book_id " +
              "FROM loans l JOIN physical_copies c ON l.copy_id = c.copy_id WHERE c.barcode = ? AND l.status = 'ACTIVE' ORDER BY l.loan_id DESC LIMIT 1";

        String updateLoanSql = "UPDATE loans SET status = 'RETURNED', returned_date = CURRENT_TIMESTAMP WHERE loan_id = ?";
        String updateCopySql = "UPDATE physical_copies SET status = 'AVAILABLE', version = version + 1 WHERE copy_id = ?";
        String incrementBookSql = "UPDATE book_titles SET available_physical_copies = available_physical_copies + 1 WHERE book_id = ?";
        String decrementMemberLoansSql = "UPDATE members SET current_loan_count = GREATEST(current_loan_count - 1, 0) WHERE member_id = ?";

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection()) {
            conn.setAutoCommit(false);
            try {
                long activeLoanId;
                long memberId;
                long copyId;
                long bookId;

                try (PreparedStatement ps = conn.prepareStatement(findLoanSql)) {
                    if (loanId != null) {
                        ps.setLong(1, loanId);
                    } else {
                        ps.setString(1, barcode);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("No active loan found for specified ID/barcode.");
                        }
                        activeLoanId = rs.getLong("loan_id");
                        memberId = rs.getLong("member_id");
                        copyId = rs.getLong("copy_id");
                        bookId = rs.getLong("book_id");
                    }
                }

                // Update loan
                try (PreparedStatement ps = conn.prepareStatement(updateLoanSql)) {
                    ps.setLong(1, activeLoanId);
                    ps.executeUpdate();
                }

                // Update copy
                try (PreparedStatement ps = conn.prepareStatement(updateCopySql)) {
                    ps.setLong(1, copyId);
                    ps.executeUpdate();
                }

                // Update book stock
                try (PreparedStatement ps = conn.prepareStatement(incrementBookSql)) {
                    ps.setLong(1, bookId);
                    ps.executeUpdate();
                }

                // Update member count
                try (PreparedStatement ps = conn.prepareStatement(decrementMemberLoansSql)) {
                    ps.setLong(1, memberId);
                    ps.executeUpdate();
                }

                conn.commit();

                Loan returnedLoan = new Loan();
                returnedLoan.setLoanId(activeLoanId);
                returnedLoan.setMemberId(memberId);
                returnedLoan.setCopyId(copyId);
                returnedLoan.setStatus(LoanStatus.RETURNED);
                returnedLoan.setReturnedDate(Instant.now());
                return returnedLoan;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
