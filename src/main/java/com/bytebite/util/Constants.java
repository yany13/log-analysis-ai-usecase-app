package com.bytebite.util;

/**
 * Application Constants
 * Centralized constants for the ByteBite application
 */
public class Constants {
    // Application info
    public static final String APP_NAME = "ByteBite";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DESCRIPTION = "Log Analysis AI Use Case";

    // Session parameters
    public static final String USER_SESSION_KEY = "user";
    public static final String SESSION_ID_KEY = "sessionId";
    public static final int SESSION_TIMEOUT_MINUTES = 30;

    // API endpoints
    public static final String API_PREFIX = "/api";
    public static final String LOGIN_ENDPOINT = "/login";
    public static final String LOGOUT_ENDPOINT = "/logout";
    public static final String MENU_ENDPOINT = "/menu";
    public static final String CHECKOUT_ENDPOINT = "/checkout";
    public static final String ANALYTICS_ENDPOINT = "/analytics";
    public static final String KDS_ENDPOINT = "/kds";
    public static final String HEALTH_ENDPOINT = "/health";

    // Security roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_KDS_OPERATOR = "KDS_OPERATOR";

    // Order status
    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_PROCESSING = "PROCESSING";
    public static final String ORDER_STATUS_READY = "READY";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    // KDS status
    public static final String KDS_STATUS_NEW = "NEW";
    public static final String KDS_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String KDS_STATUS_READY = "READY";
    public static final String KDS_STATUS_COMPLETED = "COMPLETED";

    // Event types
    public static final String EVENT_LOGIN = "LOGIN";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_UPDATED = "ORDER_UPDATED";
    public static final String EVENT_MENU_VIEWED = "MENU_VIEWED";
    public static final String EVENT_CHECKOUT = "CHECKOUT";

    // Log levels
    public static final String LOG_LEVEL_DEBUG = "DEBUG";
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_ERROR = "ERROR";

    // Header names
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_ACCEPT = "Accept";

    // MIME types
    public static final String MIME_JSON = "application/json";
    public static final String MIME_HTML = "text/html";
    public static final String MIME_XML = "application/xml";
    public static final String MIME_FORM = "application/x-www-form-urlencoded";

    // Database schema
    public static final String DB_SCHEMA = "bytebite";

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Failure injection
    public static final String FAILURE_INJECTION_ENABLED_KEY = "failure.injection.enabled";
}
