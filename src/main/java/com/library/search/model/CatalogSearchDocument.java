package com.library.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Document model mapped to the Elasticsearch library_catalog index.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogSearchDocument {
    @JsonProperty("book_id")
    private Long bookId;

    @JsonProperty("isbn")
    private String isbn;

    @JsonProperty("isbn13")
    private String isbn13;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("authors")
    private String authors;

    @JsonProperty("publisher")
    private String publisher;

    @JsonProperty("publication_year")
    private Integer publicationYear;

    @JsonProperty("edition")
    private String edition;

    @JsonProperty("language")
    private String language;

    @JsonProperty("genre")
    private String genre;

    @JsonProperty("category")
    private String category;

    @JsonProperty("classification_code")
    private String classificationCode;

    @JsonProperty("description")
    private String description;

    @JsonProperty("total_physical_copies")
    private int totalPhysicalCopies;

    @JsonProperty("available_physical_copies")
    private int availablePhysicalCopies;

    @JsonProperty("total_digital_licences")
    private int totalDigitalLicences;

    @JsonProperty("available_digital_licences")
    private int availableDigitalLicences;

    @JsonProperty("is_available_for_checkout")
    private boolean availableForCheckout;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    @JsonProperty("indexed_at")
    private String indexedAt;

    public CatalogSearchDocument() {
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

    public boolean isAvailableForCheckout() { return availableForCheckout; }
    public void setAvailableForCheckout(boolean availableForCheckout) { this.availableForCheckout = availableForCheckout; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }
}
