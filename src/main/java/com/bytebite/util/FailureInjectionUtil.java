package com.bytebite.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Failure Injection Utility
 * Caches parsed failure type per request to avoid reading body multiple times
 */
public class FailureInjectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(FailureInjectionUtil.class);

    private static final String FAILURE_TYPE_PARAM = "failureType";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<HttpServletRequest, String> failureTypeCache = new WeakHashMap<>();

    // Failure type constants
    public static final String FAILURE_DB_LEAK = "db_leak";
    public static final String FAILURE_LDAP_TIMEOUT = "ldap_timeout";
    public static final String FAILURE_OOM = "oom";
    public static final String FAILURE_DEADLOCK = "deadlock";
    public static final String FAILURE_SSL = "ssl";

    /**
     * Get failure type from JSON request body (cached per request)
     */
    public static String getFailureType(HttpServletRequest request) {
        // Return cached value if available
        if (failureTypeCache.containsKey(request)) {
            return failureTypeCache.get(request);
        }

        String failureType = null;
        try {
            if (request.getContentType() != null && request.getContentType().contains("application/json")) {
                String body = new String(request.getInputStream().readAllBytes());
                if (body != null && !body.isEmpty()) {
                    Map<String, Object> data = objectMapper.readValue(body, Map.class);
                    failureType = (String) data.get(FAILURE_TYPE_PARAM);
                }
            }
        } catch (Exception e) {
            // Silently ignore parsing errors
        }

        // Cache the result (null is also cached)
        failureTypeCache.put(request, failureType);
        return failureType;
    }

    /**
     * Check if any failure injection is requested
     */
    public static boolean isFailureRequested(HttpServletRequest request) {
        return getFailureType(request) != null;
    }

    /**
     * Check if a specific failure type is requested
     */
    public static boolean isFailureType(HttpServletRequest request, String failureType) {
        String requestedFailure = getFailureType(request);
        return failureType.equals(requestedFailure);
    }

    /**
     * Log failure injection activation
     */
    public static void logFailureActivation(String failureType) {
        logger.warn("⚠️ FAILURE INJECTION ACTIVATED: {}", failureType);
    }

    /**
     * Log failure injection completion
     */
    public static void logFailureCompletion(String failureType) {
        logger.error("❌ FAILURE SCENARIO COMPLETE: {}", failureType);
    }
}
