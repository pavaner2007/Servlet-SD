package com.library.api.servlet;

import com.library.api.common.JsonHelper;
import com.library.cron.OverdueReminderCronJob;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Admin Endpoint to manually trigger batch cron jobs for QA testing and operational maintenance.
 * POST /api/v1/admin/jobs/trigger-overdue-reminders
 */
@WebServlet(name = "AdminJobServlet", urlPatterns = {"/api/v1/admin/jobs/trigger-overdue-reminders"})
public class AdminJobServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OverdueReminderCronJob cronJob = OverdueReminderCronJob.getInstance();
        Map<String, Object> result = cronJob.executeScan();
        JsonHelper.sendSuccess(response, HttpServletResponse.SC_OK, result);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }
}
