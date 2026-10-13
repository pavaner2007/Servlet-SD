package com.library.api.servlet;

import com.library.api.common.JsonHelper;
import com.library.catalog.service.CatalogService;
import com.library.model.BookTitle;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Endpoint: GET /api/v1/catalog/search
 */
@WebServlet(name = "CatalogApiServlet", urlPatterns = {"/api/v1/catalog/search"})
public class CatalogApiServlet extends HttpServlet {

    private final CatalogService catalogService = new CatalogService();
    private final com.library.cache.RedisCacheService redisCacheService = new com.library.cache.RedisCacheService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("q");
        String genre = request.getParameter("genre");
        String category = request.getParameter("category");
        boolean onlyAvailable = "true".equalsIgnoreCase(request.getParameter("onlyAvailable"));

        int page = 1;
        int limit = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
            if (request.getParameter("limit") != null) {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
        } catch (NumberFormatException ignored) {}

        // Construct unique cache key
        String cacheKey = String.format("q=%s:genre=%s:cat=%s:avail=%s:p=%d:l=%d",
                query != null ? query : "*",
                genre != null ? genre : "*",
                category != null ? category : "*",
                onlyAvailable,
                page,
                limit);

        // Check Redis Cache
        String cachedJson = redisCacheService.getCachedSearchResults(cacheKey);
        if (cachedJson != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("X-Cache", "HIT");
            response.getWriter().write(cachedJson);
            return;
        }

        try {
            List<BookTitle> books = catalogService.searchBooks(query, genre, category, onlyAvailable, page, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("limit", limit);
            result.put("count", books.size());
            result.put("books", books);

            String jsonPayload = JsonHelper.getMapper().writeValueAsString(result);

            // Populate Redis Cache
            redisCacheService.setCachedSearchResults(cacheKey, jsonPayload);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("X-Cache", "MISS");
            response.getWriter().write(jsonPayload);

        } catch (SQLException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", e.getMessage(), null);
        }
    }
}
