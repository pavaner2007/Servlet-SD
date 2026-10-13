package com.library.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bibliographic master entity representing a book title in the catalog.
 */
public class BookTitle {
    private Long bookId;
    private String isbn;
    private String isbn13;
    private String title;
    private String subtitle;
    private String authors;
    private String publisher;
    private Integer publicationYear;
    private String edition;
    private String language;
    private String genre;
    private String category;
    private String classificationCode;
    private String description;
    private int totalPhysicalCopies;
    private int availablePhysicalCopies;
    private int totalDigitalLicences;
    private int availableDigitalLicences;
    private String coverImageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    // Associated item relationships
    private List<PhysicalCopy> physicalCopies = new ArrayList<>();
    private List<DigitalLicence> digitalLicences = new ArrayList<>();

    public BookTitle() {
    }

    public BookTitle(Long bookId, String isbn, String title, String authors) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.language = "English";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean hasAvailableCopy() {
        return availablePhysicalCopies > 0 || availableDigitalLicences > 0;
    }

    // Getters and Setters
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getClassificationCode() { return classificationCode; }
    public void setClassificationCode(String classificationCode) { this.classificationCode = classificationCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTotalPhysicalCopies() { return totalPhysicalCopies; }
    public void setTotalPhysicalCopies(int totalPhysicalCopies) { this.totalPhysicalCopies = totalPhysicalCopies; }

    public int getAvailablePhysicalCopies() { return availablePhysicalCopies; }
    public void setAvailablePhysicalCopies(int availablePhysicalCopies) { this.availablePhysicalCopies = availablePhysicalCopies; }

    public int getTotalDigitalLicences() { return totalDigitalLicences; }
    public void setTotalDigitalLicences(int totalDigitalLicences) { this.totalDigitalLicences = totalDigitalLicences; }

    public int getAvailableDigitalLicences() { return availableDigitalLicences; }
    public void setAvailableDigitalLicences(int availableDigitalLicences) { this.availableDigitalLicences = availableDigitalLicences; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<PhysicalCopy> getPhysicalCopies() { return physicalCopies; }
    public void setPhysicalCopies(List<PhysicalCopy> physicalCopies) { this.physicalCopies = physicalCopies; }

    public List<DigitalLicence> getDigitalLicences() { return digitalLicences; }
    public void setDigitalLicences(List<DigitalLicence> digitalLicences) { this.digitalLicences = digitalLicences; }
}
