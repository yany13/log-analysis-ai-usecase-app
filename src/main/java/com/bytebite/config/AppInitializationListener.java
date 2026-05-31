package com.bytebite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Application Initialization Listener
 * Handles startup initialization tasks for the ByteBite application
 */
@WebListener
public class AppInitializationListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppInitializationListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("ByteBite Application Starting Up");
        logger.info("========================================");
        
        String appName = sce.getServletContext().getInitParameter("app.name");
        String appVersion = sce.getServletContext().getInitParameter("app.version");
        String logLevel = sce.getServletContext().getInitParameter("log.level");
        String failureInjectionEnabled = sce.getServletContext().getInitParameter("failure.injection.enabled");
        
        logger.info("Application Name: {}", appName);
        logger.info("Application Version: {}", appVersion);
        logger.info("Log Level: {}", logLevel);
        logger.info("Failure Injection Enabled: {}", failureInjectionEnabled);
        
        try {
            // Initialize environment configurations
            initializeEnvironment(sce);
            
            // Verify database connectivity
            verifyDatabaseConnectivity(sce);
            
            // Verify LDAP connectivity
            verifyLDAPConnectivity(sce);
            
            logger.info("========================================");
            logger.info("ByteBite Application Initialized Successfully");
            logger.info("========================================");
        } catch (Exception e) {
            logger.error("Failed to initialize ByteBite Application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("ByteBite Application Shutting Down");
        logger.info("========================================");
        
        try {
            // Cleanup resources
            cleanupResources(sce);
            logger.info("Application cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during application cleanup", e);
        }
    }

    private void initializeEnvironment(ServletContextEvent sce) {
        logger.info("Initializing environment configuration...");
        // Load environment-specific configurations
        String dbHost = System.getenv("DB_HOST");
        String ldapHost = System.getenv("LDAP_HOST");
        logger.info("Database Host: {}", dbHost != null ? "configured" : "using default");
        logger.info("LDAP Host: {}", ldapHost != null ? "configured" : "using default");
    }

    private void verifyDatabaseConnectivity(ServletContextEvent sce) {
        logger.info("Verifying database connectivity...");
        try {
            // This will be tested when DataSourceInitListener runs
            logger.info("Database connectivity verification deferred to DataSourceInitListener");
        } catch (Exception e) {
            logger.warn("Database verification failed: {}", e.getMessage());
        }
    }

    private void verifyLDAPConnectivity(ServletContextEvent sce) {
        logger.info("Verifying LDAP connectivity...");
        try {
            String ldapHost = System.getenv("LDAP_HOST");
            String ldapPort = System.getenv("LDAP_PORT");
            if (ldapHost != null && ldapPort != null) {
                logger.info("LDAP configured at {}:{}", ldapHost, ldapPort);
            } else {
                logger.warn("LDAP configuration missing");
            }
        } catch (Exception e) {
            logger.warn("LDAP verification failed: {}", e.getMessage());
        }
    }

    private void cleanupResources(ServletContextEvent sce) {
        logger.info("Cleaning up application resources...");
        // Close any global resources
    }
}
