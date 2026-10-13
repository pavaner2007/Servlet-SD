package com.library.api.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.api.common.JsonHelper;
import com.library.circulation.service.CirculationService;
import com.library.model.Loan;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * REST Endpoint: PUT /api/v1/loans/return (also handles POST)
 */
@WebServlet(name = "LoanReturnApiServlet", urlPatterns = {"/api/v1/loans/return"})
public class LoanReturnApiServlet extends HttpServlet {

    private final CirculationService circulationService = new CirculationService();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processReturn(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processReturn(request, response);
    }

    private void processReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonNode root;
        try {
            root = JsonHelper.getMapper().readTree(request.getInputStream());
        } catch (Exception e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body must be valid JSON.", null);
            return;
        }

        Long loanId = (root != null && root.has("loanId")) ? root.get("loanId").asLong() : null;
        String barcode = (root != null && root.has("barcode")) ? root.get("barcode").asText().trim() : null;

        if (loanId == null && (barcode == null || barcode.isEmpty())) {
            JsonHelper.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_FIELDS", "Either 'loanId' or 'barcode' must be provided.", null);
            return;
        }

        try {
            Loan returnedLoan = circulationService.returnCopy(loanId, barcode);
            JsonHelper.sendSuccess(response, HttpServletResponse.SC_OK, returnedLoan);

        } catch (IllegalArgumentException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_NOT_FOUND, "LOAN_NOT_FOUND", e.getMessage(), null);
        } catch (SQLException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", e.getMessage(), null);
        }
    }
}
