package com.bytebite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Failure Injection Utility
 * Provides centralized control for intentional failure injection in troubleshooting scenarios
 */
public class FailureInjectionUtil {
    private static final Logger logger = LoggerFactory.getLogger(FailureInjectionUtil.class);

    private static final String GLOBAL_FAILURE_INJECTION_KEY = "failure.injection.enabled";
    private static final String REQUEST_PARAM_KEY = "injectFailure";
    private static final String REQUEST_PARAM_TYPE = "failureType";

    // Failure type constants
    public static final String FAILURE_DB_LEAK = "db_leak";
    public static final String FAILURE_LDAP_TIMEOUT = "ldap_timeout";
    public static final String FAILURE_OOM = "oom";
    public static final String FAILURE_DEADLOCK = "deadlock";
    public static final String FAILURE_SSL = "ssl";

    /**
     * Check if failure injection is globally enabled via environment or system property
     */
    public static boolean isGloballyEnabled() {
        String enabled = System.getenv("FAILURE_INJECTION_ENABLED");
        if (enabled == null) {
            enabled = System.getProperty(GLOBAL_FAILURE_INJECTION_KEY, "false");
        }
        return "true".equalsIgnoreCase(enabled);
    }

    /**
     * Check if failure injection is requested via request parameter
     * Request parameter overrides global setting
     */
    public static boolean isFailureInjectionRequested(HttpServletRequest request) {
        String injectParam = request.getParameter(REQUEST_PARAM_KEY);
        if (injectParam != null) {
            return "true".equalsIgnoreCase(injectParam);
        }
        return isGloballyEnabled();
    }

    /**
     * Get the specific failure type requested, if any
     */
    public static String getRequestedFailureType(HttpServletRequest request) {
        return request.getParameter(REQUEST_PARAM_TYPE);
    }

    /**
     * Check if a specific failure type is requested
     */
    public static boolean isSpecificFailureRequested(HttpServletRequest request, String failureType) {
        if (!isFailureInjectionRequested(request)) {
            return false;
        }
        String requested = getRequestedFailureType(request);
        return failureType.equals(requested);
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
