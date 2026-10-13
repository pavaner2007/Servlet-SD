package com.library.api.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Common JSON Utility for REST Servlets.
 */
public class JsonHelper {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    public static void sendSuccess(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), data);
    }

    public static void sendError(HttpServletResponse response, int statusCode, String errorCode, String message, Object details) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("status", statusCode);
        errorMap.put("errorCode", errorCode);
        errorMap.put("message", message);
        if (details != null) {
            errorMap.put("details", details);
        }
        mapper.writeValue(response.getWriter(), errorMap);
    }
}
