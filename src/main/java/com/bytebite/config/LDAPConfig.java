package com.bytebite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDAP Configuration
 * Centralizes LDAP configuration constants for authentication
 */
public class LDAPConfig {
    private static final Logger logger = LoggerFactory.getLogger(LDAPConfig.class);

    // LDAP connection parameters
    public static final String LDAP_HOST = getEnv("LDAP_HOST", "localhost");
    public static final String LDAP_PORT = getEnv("LDAP_PORT", "389");
    public static final String LDAP_BASE_DN = getEnv("LDAP_BASE_DN", "dc=bytebite,dc=local");
    public static final String LDAP_BIND_DN = getEnv("LDAP_BIND_DN", "cn=admin,dc=bytebite,dc=local");
    public static final String LDAP_BIND_PASSWORD = getEnv("LDAP_BIND_PASSWORD", "admin_secure_pwd_123");

    // LDAP connection pool parameters
    public static final int CONNECTION_POOL_MAX_SIZE = 20;
    public static final int CONNECTION_POOL_PREFSIZE = 5;
    public static final int CONNECTION_POOL_TIMEOUT = 300000;

    // LDAP user search parameters
    public static final String USER_OU = "ou=people";
    public static final String USER_OBJECT_CLASS = "inetOrgPerson";
    public static final String USER_SEARCH_FILTER = "(&(uid={0})(objectClass=inetOrgPerson))";
    public static final String USER_DN_PATTERN = "uid={0},ou=people," + LDAP_BASE_DN;

    // LDAP group search parameters
    public static final String GROUP_OU = "ou=groups";
    public static final String GROUP_OBJECT_CLASS = "groupOfNames";

    // LDAP URL
    public static final String LDAP_URL = String.format(
        "ldap://%s:%s",
        LDAP_HOST, LDAP_PORT
    );

    // LDAPS (secure) URL
    public static final String LDAPS_URL = String.format(
        "ldaps://%s:636",
        LDAP_HOST
    );

    static {
        logger.info("LDAP Configuration loaded:");
        logger.info("  Host: {}", LDAP_HOST);
        logger.info("  Port: {}", LDAP_PORT);
        logger.info("  Base DN: {}", LDAP_BASE_DN);
        logger.info("  Bind DN: {}", LDAP_BIND_DN);
        logger.info("  LDAP URL: {}", LDAP_URL);
        logger.info("  Connection pool size: {} (prefsize: {}, timeout: {}ms)",
                    CONNECTION_POOL_MAX_SIZE, CONNECTION_POOL_PREFSIZE, CONNECTION_POOL_TIMEOUT);
    }

    public static String getLdapUrl() {
        return LDAP_URL;
    }

    public static String getLdapsUrl() {
        return LDAPS_URL;
    }

    public static String getBindDn() {
        return LDAP_BIND_DN;
    }

    public static String getBindPassword() {
        return LDAP_BIND_PASSWORD;
    }

    public static String getBaseDn() {
        return LDAP_BASE_DN;
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
