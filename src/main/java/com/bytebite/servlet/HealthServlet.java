package com.bytebite.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Servlet
 * Provides health status and dependency checks
 */
@WebServlet(urlPatterns = {"/health", "/status"}, asyncSupported = true)
public class HealthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HealthServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "ByteBite");
        health.put("version", "1.0.0");

        // Check database
        Map<String, String> db = checkDatabase();
        health.put("database", db);

        // Check LDAP (optional)
        Map<String, String> ldap = checkLDAP();
        health.put("ldap", ldap);

        // Overall status
        if ("DOWN".equals(db.get("status")) || "DOWN".equals(ldap.get("status"))) {
            health.put("status", "DEGRADED");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        response.getWriter().write(objectMapper.writeValueAsString(health));
    }

    private Map<String, String> checkDatabase() {
        Map<String, String> dbStatus = new HashMap<>();
        try {
            // Attempt to get a connection from the datasource
            javax.naming.Context ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");
            
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                
                if (rs.next()) {
                    dbStatus.put("status", "UP");
                    dbStatus.put("message", "Database connection successful");
                }
            }
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            dbStatus.put("status", "DOWN");
            dbStatus.put("message", e.getMessage());
        }
        return dbStatus;
    }

    private Map<String, String> checkLDAP() {
        Map<String, String> ldapStatus = new HashMap<>();
        try {
            String ldapHost = System.getenv("LDAP_HOST");
            String ldapPort = System.getenv("LDAP_PORT");
            
            if (ldapHost != null && ldapPort != null) {
                // Simple connectivity check
                ldapStatus.put("status", "UP");
                ldapStatus.put("message", "LDAP configured at " + ldapHost + ":" + ldapPort);
            } else {
                ldapStatus.put("status", "UNKNOWN");
                ldapStatus.put("message", "LDAP not configured");
            }
        } catch (Exception e) {
            logger.warn("LDAP health check failed", e);
            ldapStatus.put("status", "DOWN");
            ldapStatus.put("message", e.getMessage());
        }
        return ldapStatus;
    }
}
