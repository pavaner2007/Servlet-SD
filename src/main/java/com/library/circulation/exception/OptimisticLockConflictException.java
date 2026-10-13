package com.library.circulation.exception;

/**
 * Thrown when an optimistic concurrency conflict occurs during checkout.
 * Results in HTTP 409 Conflict.
 */
public class OptimisticLockConflictException extends RuntimeException {
    private final String barcode;
    private final int attemptedVersion;
    private final int currentVersion;
    private final String currentStatus;

    public OptimisticLockConflictException(String message, String barcode, int attemptedVersion, int currentVersion, String currentStatus) {
        super(message);
        this.barcode = barcode;
        this.attemptedVersion = attemptedVersion;
        this.currentVersion = currentVersion;
        this.currentStatus = currentStatus;
    }

    public String getBarcode() { return barcode; }
    public int getAttemptedVersion() { return attemptedVersion; }
    public int getCurrentVersion() { return currentVersion; }
    public String getCurrentStatus() { return currentStatus; }
}
