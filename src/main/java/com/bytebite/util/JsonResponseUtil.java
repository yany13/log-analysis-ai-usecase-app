package com.bytebite.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * JSON Response Utility
 * Provides consistent JSON response formatting
 */
public class JsonResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonResponseUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public static Map<String, Object> success(Object data, String message) {
        Map<String, Object> response = success(data);
        response.put("message", message);
        return response;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public static Map<String, Object> error(String message, String code) {
        Map<String, Object> response = error(message);
        response.put("code", code);
        return response;
    }

    public static Map<String, Object> error(String message, String code, Object details) {
        Map<String, Object> response = error(message, code);
        response.put("details", details);
        return response;
    }

    public static Map<String, Object> validation(List<String> errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "validation_error");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    public static Map<String, Object> paginated(List<?> items, int page, int total) {
        Map<String, Object> response = success(items);
        response.put("page", page);
        response.put("total", total);
        response.put("pages", (int) Math.ceil((double) total / Constants.DEFAULT_PAGE_SIZE));
        return response;
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Error serializing to JSON", e);
            return "{}";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("Error deserializing from JSON", e);
            return null;
        }
    }
}
