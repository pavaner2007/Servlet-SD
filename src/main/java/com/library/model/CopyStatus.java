package com.library.model;

/**
 * Status of an individual physical book inventory copy.
 */
public enum CopyStatus {
    AVAILABLE,
    LOANED,
    RESERVED,
    LOST;

    public boolean isBorrowable() {
        return this == AVAILABLE;
    }
}
