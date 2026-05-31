package com.bytebite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Failure Injection Utility
 * Provides centralized control for intentional failure injection via HTTP request parameters
 */
public class FailureInjectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(FailureInjectionUtil.class);

    private static final String FAILURE_TYPE_PARAM = "failureType";

    // Failure type constants
    public static final String FAILURE_DB_LEAK = "db_leak";
    public static final String FAILURE_LDAP_TIMEOUT = "ldap_timeout";
    public static final String FAILURE_OOM = "oom";
    public static final String FAILURE_DEADLOCK = "deadlock";
    public static final String FAILURE_SSL = "ssl";

    /**
     * Get the failure type requested via HTTP parameter
     */
    public static String getFailureType(HttpServletRequest request) {
        return request.getParameter(FAILURE_TYPE_PARAM);
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
