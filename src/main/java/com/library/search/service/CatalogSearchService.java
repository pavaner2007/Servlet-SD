package com.library.search.service;

import com.library.model.BookTitle;
import com.library.search.model.CatalogSearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service orchestrating catalogue synchronization and search operations with Elasticsearch.
 */
public class CatalogSearchService {
    private static final Logger logger = LoggerFactory.getLogger(CatalogSearchService.class);
    private final String indexName;

    public CatalogSearchService() {
        this.indexName = "library_catalog";
    }

    public CatalogSearchService(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Transforms a relational BookTitle database entity into an Elasticsearch search document.
     */
    public CatalogSearchDocument toSearchDocument(BookTitle book) {
        CatalogSearchDocument doc = new CatalogSearchDocument();
        doc.setBookId(book.getBookId());
        doc.setIsbn(book.getIsbn());
        doc.setIsbn13(book.getIsbn13());
        doc.setTitle(book.getTitle());
        doc.setSubtitle(book.getSubtitle());
        doc.setAuthors(book.getAuthors());
        doc.setPublisher(book.getPublisher());
        doc.setPublicationYear(book.getPublicationYear());
        doc.setEdition(book.getEdition());
        doc.setLanguage(book.getLanguage());
        doc.setGenre(book.getGenre());
        doc.setCategory(book.getCategory());
        doc.setClassificationCode(book.getClassificationCode());
        doc.setDescription(book.getDescription());
        doc.setTotalPhysicalCopies(book.getTotalPhysicalCopies());
        doc.setAvailablePhysicalCopies(book.getAvailablePhysicalCopies());
        doc.setTotalDigitalLicences(book.getTotalDigitalLicences());
        doc.setAvailableDigitalLicences(book.getAvailableDigitalLicences());
        doc.setAvailableForCheckout(book.hasAvailableCopy());
        doc.setCoverImageUrl(book.getCoverImageUrl());
        doc.setIndexedAt(Instant.now().toString());

        if (book.getGenre() != null || book.getCategory() != null) {
            List<String> tags = new ArrayList<>();
            if (book.getGenre() != null) tags.add(book.getGenre().toLowerCase());
            if (book.getCategory() != null) tags.add(book.getCategory().toLowerCase());
            doc.setTags(tags);
        }

        return doc;
    }

    /**
     * Builds the JSON DSL query string for fast multi-match search with field boosting.
     */
    public String buildSearchQueryJson(String queryText, boolean onlyAvailable) {
        return String.format(
            "{\n" +
            "  \"query\": {\n" +
            "    \"bool\": {\n" +
            "      \"must\": [\n" +
            "        {\n" +
            "          \"multi_match\": {\n" +
            "            \"query\": \"%s\",\n" +
            "            \"fields\": [\"title^4\", \"authors^3\", \"subtitle^2\", \"description^1\"],\n" +
            "            \"type\": \"best_fields\",\n" +
            "            \"fuzziness\": \"AUTO\"\n" +
            "          }\n" +
            "        }\n" +
            "      ]%s\n" +
            "    }\n" +
            "  }\n" +
            "}",
            escapeJson(queryText),
            onlyAvailable ? ",\n      \"filter\": [ { \"term\": { \"is_available_for_checkout\": true } } ]" : ""
        );
    }

    /**
     * Builds the JSON DSL query for fast autocomplete.
     */
    public String buildAutocompleteQueryJson(String prefixText, int limit) {
        return String.format(
            "{\n" +
            "  \"_source\": [\"book_id\", \"title\", \"authors\", \"cover_image_url\"],\n" +
            "  \"query\": {\n" +
            "    \"multi_match\": {\n" +
            "      \"query\": \"%s\",\n" +
            "      \"type\": \"bool_prefix\",\n" +
            "      \"fields\": [\"title.autocomplete\", \"authors.autocomplete\"]\n" +
            "    }\n" +
            "  },\n" +
            "  \"size\": %d\n" +
            "}",
            escapeJson(prefixText),
            limit
        );
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String getIndexName() {
        return indexName;
    }
}
