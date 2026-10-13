package com.library.api.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.api.common.JsonHelper;
import com.library.circulation.exception.OptimisticLockConflictException;
import com.library.circulation.service.CirculationService;
import com.library.model.Loan;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Endpoint: POST /api/v1/loans/checkout
 * Implements Optimistic Locking on scarce copies and returns HTTP 409 Conflict on concurrency clashes.
 */
@WebServlet(name = "LoanCheckoutApiServlet", urlPatterns = {"/api/v1/loans/checkout"})
public class LoanCheckoutApiServlet extends HttpServlet {

    private final CirculationService circulationService = new CirculationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonNode root;
        try {
            root = JsonHelper.getMapper().readTree(request.getInputStream());
        } catch (Exception e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body must be valid JSON.", null);
            return;
        }

        if (root == null || !root.has("memberId") || !root.has("barcode")) {
            JsonHelper.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_FIELDS", "Both 'memberId' and 'barcode' are required.", null);
            return;
        }

        long memberId = root.get("memberId").asLong();
        String barcode = root.get("barcode").asText().trim();
        Integer expectedVersion = root.has("version") ? root.get("version").asInt() : null;
        int durationDays = root.has("durationDays") ? root.get("durationDays").asInt(14) : 14;

        try {
            Loan loan = circulationService.checkoutCopy(memberId, barcode, expectedVersion, durationDays);
            JsonHelper.sendSuccess(response, HttpServletResponse.SC_CREATED, loan);

        } catch (OptimisticLockConflictException e) {
            // Crucial: Return HTTP 409 Conflict with concurrency conflict diagnostics
            Map<String, Object> conflictDetails = new HashMap<>();
            conflictDetails.put("barcode", e.getBarcode());
            conflictDetails.put("attemptedVersion", e.getAttemptedVersion());
            conflictDetails.put("currentVersion", e.getCurrentVersion());
            conflictDetails.put("currentStatus", e.getCurrentStatus());
            conflictDetails.put("resolution", "Please refresh the copy details and re-attempt checkout or join waitlist.");

            JsonHelper.sendError(response, HttpServletResponse.SC_CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", e.getMessage(), conflictDetails);

        } catch (IllegalArgumentException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), null);
        } catch (IllegalStateException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_FORBIDDEN, "LOAN_RESTRICTION", e.getMessage(), null);
        } catch (SQLException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", e.getMessage(), null);
        }
    }
}
