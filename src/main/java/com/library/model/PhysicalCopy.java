package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Physical inventory unit with status constraint: AVAILABLE, LOANED, RESERVED, LOST.
 */
public class PhysicalCopy {
    private Long copyId;
    private Long bookId;
    private String barcode;
    private String rfidTag;
    private String shelfLocation;
    private String conditionNote;
    private CopyStatus status; // AVAILABLE, LOANED, RESERVED, LOST
    private LocalDate acquiredDate;
    private BigDecimal price;
    private int version = 1;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional eager reference
    private BookTitle bookTitle;

    public PhysicalCopy() {
        this.status = CopyStatus.AVAILABLE;
    }

    public PhysicalCopy(Long copyId, Long bookId, String barcode, String shelfLocation, CopyStatus status) {
        this.copyId = copyId;
        this.bookId = bookId;
        this.barcode = barcode;
        this.shelfLocation = shelfLocation;
        this.status = status != null ? status : CopyStatus.AVAILABLE;
        this.acquiredDate = LocalDate.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Status transition helpers
    public void markAsLoaned() {
        if (this.status != CopyStatus.AVAILABLE && this.status != CopyStatus.RESERVED) {
            throw new IllegalStateException("Cannot loan a copy in status: " + this.status);
        }
        this.status = CopyStatus.LOANED;
        this.updatedAt = Instant.now();
    }

    public void markAsReturned() {
        this.status = CopyStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void markAsReserved() {
        if (this.status != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE copies can be reserved");
        }
        this.status = CopyStatus.RESERVED;
        this.updatedAt = Instant.now();
    }

    public void markAsLost() {
        this.status = CopyStatus.LOST;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getRfidTag() { return rfidTag; }
    public void setRfidTag(String rfidTag) { this.rfidTag = rfidTag; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String conditionNote) { this.conditionNote = conditionNote; }

    public CopyStatus getStatus() { return status; }
    public void setStatus(CopyStatus status) { this.status = status; }

    public LocalDate getAcquiredDate() { return acquiredDate; }
    public void setAcquiredDate(LocalDate acquiredDate) { this.acquiredDate = acquiredDate; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public BookTitle getBookTitle() { return bookTitle; }
    public void setBookTitle(BookTitle bookTitle) { this.bookTitle = bookTitle; }
}
