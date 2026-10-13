package com.library.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Reservation object model for managing waitlists and holds on book titles.
 */
public class Reservation {
    private Long reservationId;
    private Long memberId;
    private Long bookId;
    private Long allocatedCopyId;
    private Instant reservationDate;
    private int queuePosition;
    private Instant expiryDate;
    private Instant notifiedAt;
    private ReservationStatus status; // PENDING, FULFILLED, CANCELLED, EXPIRED
    private Instant createdAt;
    private Instant updatedAt;

    // Associated eager relationships
    private Member member;
    private BookTitle bookTitle;
    private PhysicalCopy allocatedCopy;

    public Reservation() {
        this.status = ReservationStatus.PENDING;
        this.queuePosition = 1;
    }

    public static Reservation create(Long memberId, Long bookId, int holdDays) {
        Reservation reservation = new Reservation();
        reservation.setMemberId(memberId);
        reservation.setBookId(bookId);
        reservation.setReservationDate(Instant.now());
        reservation.setExpiryDate(Instant.now().plus(holdDays, ChronoUnit.DAYS));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(Instant.now());
        reservation.setUpdatedAt(Instant.now());
        return reservation;
    }

    public void allocateCopy(Long copyId) {
        this.allocatedCopyId = copyId;
        this.notifiedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fulfill() {
        this.status = ReservationStatus.FULFILLED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return this.status == ReservationStatus.PENDING &&
               Instant.now().isAfter(this.expiryDate);
    }

    // Getters and Setters
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getAllocatedCopyId() { return allocatedCopyId; }
    public void setAllocatedCopyId(Long allocatedCopyId) { this.allocatedCopyId = allocatedCopyId; }

    public Instant getReservationDate() { return reservationDate; }
    public void setReservationDate(Instant reservationDate) { this.reservationDate = reservationDate; }

    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public BookTitle getBookTitle() { return bookTitle; }
    public void setBookTitle(BookTitle bookTitle) { this.bookTitle = bookTitle; }

    public PhysicalCopy getAllocatedCopy() { return allocatedCopy; }
    public void setAllocatedCopy(PhysicalCopy allocatedCopy) { this.allocatedCopy = allocatedCopy; }
}
