package com.library.model;

import java.time.Instant;

/**
 * Digital Rights Management licence model for electronic / digital checkouts.
 */
public class DigitalLicence {
    private Long licenceId;
    private Long bookId;
    private String licenceKey;
    private String provider;
    private String format; // EPUB, PDF, AUDIOBOOK
    private Instant validFrom;
    private Instant validTo;
    private int maxConcurrentCheckouts;
    private int activeCheckouts;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public DigitalLicence() {
        this.maxConcurrentCheckouts = 1;
        this.activeCheckouts = 0;
        this.active = true;
    }

    public DigitalLicence(Long licenceId, Long bookId, String licenceKey, String provider, int maxConcurrentCheckouts) {
        this.licenceId = licenceId;
        this.bookId = bookId;
        this.licenceKey = licenceKey;
        this.provider = provider;
        this.format = "EPUB";
        this.validFrom = Instant.now();
        this.maxConcurrentCheckouts = maxConcurrentCheckouts;
        this.activeCheckouts = 0;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean hasAvailableSlot() {
        return active &&
               (validTo == null || validTo.isAfter(Instant.now())) &&
               activeCheckouts < maxConcurrentCheckouts;
    }

    public void incrementCheckout() {
        if (!hasAvailableSlot()) {
            throw new IllegalStateException("No digital licence slots available for checkout");
        }
        this.activeCheckouts++;
        this.updatedAt = Instant.now();
    }

    public void decrementCheckout() {
        if (this.activeCheckouts > 0) {
            this.activeCheckouts--;
            this.updatedAt = Instant.now();
        }
    }

    // Getters and Setters
    public Long getLicenceId() { return licenceId; }
    public void setLicenceId(Long licenceId) { this.licenceId = licenceId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getLicenceKey() { return licenceKey; }
    public void setLicenceKey(String licenceKey) { this.licenceKey = licenceKey; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }

    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }

    public int getMaxConcurrentCheckouts() { return maxConcurrentCheckouts; }
    public void setMaxConcurrentCheckouts(int maxConcurrentCheckouts) { this.maxConcurrentCheckouts = maxConcurrentCheckouts; }

    public int getActiveCheckouts() { return activeCheckouts; }
    public void setActiveCheckouts(int activeCheckouts) { this.activeCheckouts = activeCheckouts; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
