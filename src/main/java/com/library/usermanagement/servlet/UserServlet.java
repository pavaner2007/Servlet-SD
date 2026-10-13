package com.library.usermanagement.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;

/**
 * UserServlet: Handles HTTP POST requests for CRUD operations on MySQL 'users' table.
 * Supported actions: Add (INSERT), View (SELECT), Update (UPDATE), Delete (DELETE).
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/UserServlet"})
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Database Connection Parameters
    private String jdbcUrl = "jdbc:mysql://localhost:3306/user_auth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private String jdbcUsername = "root";
    private String jdbcPassword = "password";
    private String jdbcDriver = "com.mysql.cj.jdbc.Driver";

    @Override
    public void init() throws ServletException {
        // Attempt to load database properties from classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                if (prop.containsKey("userauth.db.jdbcUrl")) {
                    this.jdbcUrl = prop.getProperty("userauth.db.jdbcUrl");
                }
                if (prop.containsKey("userauth.db.username")) {
                    this.jdbcUsername = prop.getProperty("userauth.db.username");
                }
                if (prop.containsKey("userauth.db.password")) {
                    this.jdbcPassword = prop.getProperty("userauth.db.password");
                }
            }
        } catch (Exception e) {
            getServletContext().log("Could not load db.properties, using defaults", e);
        }

        // Register JDBC Driver
        try {
            Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
            throw new ServletException("MySQL JDBC Driver not found in classpath: " + jdbcDriver, e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward GET requests to the form page or list view
        response.sendRedirect(request.getContextPath() + "/index.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String idStr = request.getParameter("id");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String action = request.getParameter("action");

        if (action == null || action.trim().isEmpty()) {
            renderResponse(response, false, "Invalid Request", "No action specified.", null, null, null);
            return;
        }

        switch (action.trim()) {
            case "Add":
                handleAddUser(request, response, idStr, name, email);
                break;
            case "View":
                handleViewUser(request, response, idStr);
                break;
            case "Update":
                handleUpdateUser(request, response, idStr, name, email);
                break;
            case "Delete":
                handleDeleteUser(request, response, idStr);
                break;
            default:
                renderResponse(response, false, "Unknown Action", "Action '" + action + "' is not supported.", null, null, null);
                break;
        }
    }

    /**
     * Action: Add -> Executes INSERT INTO users
     */
    private void handleAddUser(HttpServletRequest request, HttpServletResponse response,
                               String idStr, String name, String email) throws IOException {
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            renderResponse(response, false, "Add Failed", "Name and Email are required to add a user.", null, null, null);
            return;
        }

        boolean hasCustomId = idStr != null && !idStr.trim().isEmpty();
        String sql = hasCustomId
                ? "INSERT INTO users (id, name, email) VALUES (?, ?, ?)"
                : "INSERT INTO users (name, email) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = hasCustomId 
                     ? conn.prepareStatement(sql)
                     : conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (hasCustomId) {
                ps.setInt(1, Integer.parseInt(idStr.trim()));
                ps.setString(2, name.trim());
                ps.setString(3, email.trim());
            } else {
                ps.setString(1, name.trim());
                ps.setString(2, email.trim());
            }

            int rowsAffected = ps.executeUpdate();
            String createdId = idStr;

            if (!hasCustomId) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        createdId = String.valueOf(rs.getInt(1));
                    }
                }
            }

            if (rowsAffected > 0) {
                renderResponse(response, true, "User Added Successfully",
                        "Record inserted into database.", createdId, name, email);
            } else {
                renderResponse(response, false, "Add Failed", "No rows were inserted.", null, null, null);
            }

        } catch (NumberFormatException e) {
            renderResponse(response, false, "Input Error", "User ID must be a valid integer.", null, null, null);
        } catch (SQLIntegrityConstraintViolationException e) {
            renderResponse(response, false, "Database Conflict", "User with this ID or Email already exists: " + e.getMessage(), null, null, null);
        } catch (SQLException e) {
            renderResponse(response, false, "Database Error", "SQL Execution failed: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * Action: View -> Executes SELECT id, name, email FROM users WHERE id = ?
     */
    private void handleViewUser(HttpServletRequest request, HttpServletResponse response,
                                String idStr) throws IOException {
        if (idStr == null || idStr.trim().isEmpty()) {
            renderResponse(response, false, "View Failed", "User ID is required to view a record.", null, null, null);
            return;
        }

        String sql = "SELECT id, name, email FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int id = Integer.parseInt(idStr.trim());
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String foundName = rs.getString("name");
                    String foundEmail = rs.getString("email");
                    renderResponse(response, true, "User Found",
                            "Record retrieved successfully.", String.valueOf(id), foundName, foundEmail);
                } else {
                    renderResponse(response, false, "Not Found",
                            "No user exists in the database with ID: " + id, idStr, null, null);
                }
            }

        } catch (NumberFormatException e) {
            renderResponse(response, false, "Input Error", "User ID must be a valid integer.", null, null, null);
        } catch (SQLException e) {
            renderResponse(response, false, "Database Error", "SQL Execution failed: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * Action: Update -> Executes UPDATE users SET name = ?, email = ? WHERE id = ?
     */
    private void handleUpdateUser(HttpServletRequest request, HttpServletResponse response,
                                  String idStr, String name, String email) throws IOException {
        if (idStr == null || idStr.trim().isEmpty()) {
            renderResponse(response, false, "Update Failed", "User ID is required to update a user.", null, null, null);
            return;
        }
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            renderResponse(response, false, "Update Failed", "Name and Email are required for update.", null, null, null);
            return;
        }

        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int id = Integer.parseInt(idStr.trim());
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setInt(3, id);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                renderResponse(response, true, "User Updated Successfully",
                        "User record updated in MySQL database.", String.valueOf(id), name, email);
            } else {
                renderResponse(response, false, "Update Failed",
                        "No user found with ID: " + id + " to update.", idStr, name, email);
            }

        } catch (NumberFormatException e) {
            renderResponse(response, false, "Input Error", "User ID must be a valid integer.", null, null, null);
        } catch (SQLIntegrityConstraintViolationException e) {
            renderResponse(response, false, "Database Conflict", "Email is already taken by another user.", null, null, null);
        } catch (SQLException e) {
            renderResponse(response, false, "Database Error", "SQL Execution failed: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * Action: Delete -> Executes DELETE FROM users WHERE id = ?
     */
    private void handleDeleteUser(HttpServletRequest request, HttpServletResponse response,
                                  String idStr) throws IOException {
        if (idStr == null || idStr.trim().isEmpty()) {
            renderResponse(response, false, "Delete Failed", "User ID is required to delete a user.", null, null, null);
            return;
        }

        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int id = Integer.parseInt(idStr.trim());
            ps.setInt(1, id);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                renderResponse(response, true, "User Deleted Successfully",
                        "User ID " + id + " was permanently removed from database.", String.valueOf(id), null, null);
            } else {
                renderResponse(response, false, "Delete Failed",
                        "No user found with ID: " + id + " to delete.", idStr, null, null);
            }

        } catch (NumberFormatException e) {
            renderResponse(response, false, "Input Error", "User ID must be a valid integer.", null, null, null);
        } catch (SQLException e) {
            renderResponse(response, false, "Database Error", "SQL Execution failed: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * Helper to render a stylized HTML status response card.
     */
    private void renderResponse(HttpServletResponse response, boolean success, String title,
                                String message, String id, String name, String email) throws IOException {
        PrintWriter out = response.getWriter();
        String statusColor = success ? "#10b981" : "#ef4444";
        String statusBadge = success ? "SUCCESS" : "ERROR";

        out.println("<!DOCTYPE html>");
        out.println("<html lang='en'>");
        out.println("<head>");
        out.println("  <meta charset='UTF-8'>");
        out.println("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("  <title>" + title + " - Library System</title>");
        out.println("  <link href='https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700&display=swap' rel='stylesheet'>");
        out.println("  <style>");
        out.println("    body { background: #0f172a; color: #f8fafc; font-family: 'Plus Jakarta Sans', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; }");
        out.println("    .card { background: rgba(30, 41, 59, 0.9); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; padding: 36px; max-width: 540px; width: 100%; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }");
        out.println("    .badge { display: inline-block; padding: 4px 12px; border-radius: 99px; font-size: 0.75rem; font-weight: 700; background: " + statusColor + "22; color: " + statusColor + "; border: 1px solid " + statusColor + "; margin-bottom: 12px; }");
        out.println("    h2 { margin: 0 0 10px 0; color: #fff; font-size: 1.6rem; }");
        out.println("    p.msg { color: #94a3b8; font-size: 0.95rem; margin-bottom: 24px; }");
        out.println("    .details-table { width: 100%; border-collapse: collapse; margin-bottom: 28px; background: rgba(15, 23, 42, 0.6); border-radius: 8px; overflow: hidden; }");
        out.println("    .details-table td { padding: 12px 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); font-size: 0.9rem; }");
        out.println("    .details-table td:first-child { color: #94a3b8; font-weight: 600; width: 30%; }");
        out.println("    .btn-back { display: inline-block; text-align: center; width: 100%; padding: 12px; background: #6366f1; color: #fff; text-decoration: none; border-radius: 8px; font-weight: 600; transition: 0.2s; }");
        out.println("    .btn-back:hover { background: #4f46e5; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <div class='card'>");
        out.println("    <span class='badge'>" + statusBadge + "</span>");
        out.println("    <h2>" + title + "</h2>");
        out.println("    <p class='msg'>" + message + "</p>");

        if (id != null || name != null || email != null) {
            out.println("    <table class='details-table'>");
            if (id != null) {
                out.println("      <tr><td>User ID</td><td>" + id + "</td></tr>");
            }
            if (name != null) {
                out.println("      <tr><td>Full Name</td><td>" + name + "</td></tr>");
            }
            if (email != null) {
                out.println("      <tr><td>Email</td><td>" + email + "</td></tr>");
            }
            out.println("    </table>");
        }

        out.println("    <a href='index.html' class='btn-back'>&larr; Return to Form</a>");
        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");
    }
}
