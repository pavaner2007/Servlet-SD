package com.library.messaging.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a book is returned to the library.
 * Triggers notification service to alert members in the waitlist queue.
 */
public class BookReturnedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("bookId")
    private Long bookId;

    @JsonProperty("copyId")
    private Long copyId;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("returnedAt")
    private Instant returnedAt;

    @JsonProperty("allocatedReservationId")
    private Long allocatedReservationId;

    @JsonProperty("nextMemberId")
    private Long nextMemberId;

    public BookReturnedEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.returnedAt = Instant.now();
    }

    public BookReturnedEvent(Long loanId, Long bookId, Long copyId, String barcode) {
        this.eventId = UUID.randomUUID().toString();
        this.loanId = loanId;
        this.bookId = bookId;
        this.copyId = copyId;
        this.barcode = barcode;
        this.returnedAt = Instant.now();
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }

    public Long getAllocatedReservationId() { return allocatedReservationId; }
    public void setAllocatedReservationId(Long allocatedReservationId) { this.allocatedReservationId = allocatedReservationId; }

    public Long getNextMemberId() { return nextMemberId; }
    public void setNextMemberId(Long nextMemberId) { this.nextMemberId = nextMemberId; }

    @Override
    public String toString() {
        return "BookReturnedEvent{" +
                "eventId='" + eventId + '\'' +
                ", loanId=" + loanId +
                ", bookId=" + bookId +
                ", copyId=" + copyId +
                ", barcode='" + barcode + '\'' +
                ", returnedAt=" + returnedAt +
                ", allocatedReservationId=" + allocatedReservationId +
                ", nextMemberId=" + nextMemberId +
                '}';
    }
}
