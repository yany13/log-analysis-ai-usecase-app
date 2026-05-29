package com.bytebite.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * DataSource Initialization Listener
 * Sets up the HikariCP connection pool for PostgreSQL
 */
@WebListener
public class DataSourceInitListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceInitListener.class);
    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing DataSource with HikariCP...");
        
        try {
            // Initialize HikariCP connection pool
            initializeDataSource();
            
            // Test the connection
            testConnection();
            
            logger.info("DataSource initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize DataSource", e);
            throw new RuntimeException("DataSource initialization failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutting down DataSource...");
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("DataSource closed successfully");
            }
        } catch (Exception e) {
            logger.error("Error closing DataSource", e);
        }
    }

    private void initializeDataSource() throws NamingException {
        // Get environment variables
        String dbHost = System.getenv("DB_HOST");
        String dbPort = System.getenv("DB_PORT");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        // Fallback to defaults if not set
        dbHost = dbHost != null ? dbHost : "localhost";
        dbPort = dbPort != null ? dbPort : "5432";
        dbName = dbName != null ? dbName : "bytebite_db";
        dbUser = dbUser != null ? dbUser : "bytebite";
        dbPassword = dbPassword != null ? dbPassword : "bytebite_secure_pwd_123";

        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

        logger.info("Configuring HikariCP for: {}", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("ByteBitePool");
        config.setDriverClassName("org.postgresql.Driver");

        dataSource = new HikariDataSource(config);
    }

    private void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            String result = conn.createStatement().executeQuery("SELECT 1").next() ? "SUCCESS" : "FAILED";
            logger.info("Database connection test: {}", result);
        } catch (Exception e) {
            logger.error("Database connection test failed", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }
}
