package com.library.catalog.service;

import com.library.config.DatabaseConnectionManager;
import com.library.model.BookTitle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue Microservice: Fast search and bibliographic lookup across books.
 */
public class CatalogService {

    public List<BookTitle> searchBooks(String query, String genre, String category, boolean onlyAvailable, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT book_id, isbn, isbn13, title, subtitle, authors, publisher, publication_year, " +
            "edition, language, genre, category, description, total_physical_copies, available_physical_copies, " +
            "total_digital_licences, available_digital_licences, cover_image_url " +
            "FROM book_titles WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(authors) LIKE ? OR isbn = ? OR isbn13 = ?) ");
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(query.trim());
            params.add(query.trim());
        }

        if (genre != null && !genre.trim().isEmpty()) {
            sql.append("AND LOWER(genre) = ? ");
            params.add(genre.trim().toLowerCase());
        }

        if (category != null && !category.trim().isEmpty()) {
            sql.append("AND LOWER(category) = ? ");
            params.add(category.trim().toLowerCase());
        }

        if (onlyAvailable) {
            sql.append("AND (available_physical_copies > 0 OR available_digital_licences > 0) ");
        }

        sql.append("ORDER BY book_id ASC LIMIT ? OFFSET ?");
        params.add(Math.max(1, limit));
        params.add(Math.max(0, (page - 1) * limit));

        List<BookTitle> results = new ArrayList<>();

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookTitle book = new BookTitle();
                    book.setBookId(rs.getLong("book_id"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setIsbn13(rs.getString("isbn13"));
                    book.setTitle(rs.getString("title"));
                    book.setSubtitle(rs.getString("subtitle"));
                    book.setAuthors(rs.getString("authors"));
                    book.setPublisher(rs.getString("publisher"));
                    book.setPublicationYear(rs.getInt("publication_year"));
                    book.setEdition(rs.getString("edition"));
                    book.setLanguage(rs.getString("language"));
                    book.setGenre(rs.getString("genre"));
                    book.setCategory(rs.getString("category"));
                    book.setDescription(rs.getString("description"));
                    book.setTotalPhysicalCopies(rs.getInt("total_physical_copies"));
                    book.setAvailablePhysicalCopies(rs.getInt("available_physical_copies"));
                    book.setTotalDigitalLicences(rs.getInt("total_digital_licences"));
                    book.setAvailableDigitalLicences(rs.getInt("available_digital_licences"));
                    book.setCoverImageUrl(rs.getString("cover_image_url"));
                    results.add(book);
                }
            }
        }

        return results;
    }
}
