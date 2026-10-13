package com.library.api.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.api.common.JsonHelper;
import com.library.circulation.service.CirculationService;
import com.library.model.Reservation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * REST Endpoint: POST /api/v1/reservations
 */
@WebServlet(name = "ReservationApiServlet", urlPatterns = {"/api/v1/reservations"})
public class ReservationApiServlet extends HttpServlet {

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

        if (root == null || !root.has("memberId") || !root.has("bookId")) {
            JsonHelper.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_FIELDS", "Both 'memberId' and 'bookId' are required.", null);
            return;
        }

        long memberId = root.get("memberId").asLong();
        long bookId = root.get("bookId").asLong();
        int holdDays = root.has("holdDays") ? root.get("holdDays").asInt(3) : 3;

        try {
            Reservation reservation = circulationService.createReservation(memberId, bookId, holdDays);
            JsonHelper.sendSuccess(response, HttpServletResponse.SC_CREATED, reservation);

        } catch (IllegalArgumentException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), null);
        } catch (IllegalStateException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_FORBIDDEN, "MEMBERSHIP_RESTRICTION", e.getMessage(), null);
        } catch (SQLException e) {
            JsonHelper.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", e.getMessage(), null);
        }
    }
}
