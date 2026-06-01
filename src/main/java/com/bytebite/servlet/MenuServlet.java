package com.bytebite.servlet;

import com.bytebite.util.FailureInjectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;

/**
 * Menu Servlet
 * Handles menu operations with caching support
 */
public class MenuServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MenuServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> menuCache;
    private long cacheTime;
    private long cacheTTL;

    @Override
    public void init() throws ServletException {
        super.init();
        String cacheTTLParam = getInitParameter("cache.ttl.minutes");
        cacheTTL = (cacheTTLParam != null ? Long.parseLong(cacheTTLParam) : 30) * 60 * 1000;
        logger.info("MenuServlet initialized with cache TTL: {} minutes", cacheTTL / 60000);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/menu.html").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            String body = new String(request.getInputStream().readAllBytes());
            Map<String, Object> payload = objectMapper.readValue(body, Map.class);
            handleAPIRequest(payload, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"JSON body required\"}");
        }
    }

    private void handleAPIRequest(Map<String, Object> payload, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String failureType = (String) payload.get("failureType");
        List<Map<String, Object>> menu = getMenu(failureType);
        response.getWriter().write(objectMapper.writeValueAsString(menu));
    }

    private synchronized List<Map<String, Object>> getMenu(String failureType) {
        long now = System.currentTimeMillis();

        // Check if cache is valid (unless failure injection is requested)
        if (menuCache != null && (now - cacheTime) < cacheTTL && failureType == null) {
            logger.debug("Returning cached menu");
            return menuCache;
        }

        logger.info("Fetching menu from database");
        menuCache = buildMenu(failureType);
        cacheTime = now;
        return menuCache;
    }

    private List<Map<String, Object>> buildMenu(String failureType) {
        List<Map<String, Object>> menu = new ArrayList<>();

        // Check if DB leak failure is requested
        if (FailureInjectionUtil.FAILURE_DB_LEAK.equals(failureType)) {
            FailureInjectionUtil.logFailureActivation(FailureInjectionUtil.FAILURE_DB_LEAK);
            logger.info("MenuServlet: Attempting to load menu from database (potential connection leak scenario)");

            Connection leakedConn = null;
            try {
                javax.naming.Context ctx = new javax.naming.InitialContext();
                javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

                leakedConn = ds.getConnection();
                logger.warn("⚠️ Connection leak scenario: Got connection {} from pool but NOT closing it",
                           System.identityHashCode(leakedConn));

                String sql = "SELECT id, name, price FROM bytebite.menu_items LIMIT 100";
                try (java.sql.Statement stmt = leakedConn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(sql)) {

                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rs.getInt("id"));
                        item.put("name", rs.getString("name"));
                        item.put("price", rs.getDouble("price"));
                        item.put("available", true);
                        menu.add(item);
                    }
                }
                // Intentionally NOT closing connection - leak simulation
                logger.error("❌ CONNECTION LEAK: Connection not closed. Pool exhaustion will occur on subsequent requests");

                // Simulate pool exhaustion
                throw new java.sql.SQLException("HikariPool-1 - Connection is not available, request timed out after 30000ms (total=20, active=20, idle=0, waiting=1)");

            } catch (Exception e) {
                logger.error("❌ DATABASE CONNECTION POOL EXHAUSTED", e);
                buildHardcodedMenu(menu);
            }
        } else {
            buildHardcodedMenu(menu);
        }

        return menu;
    }

    private void buildHardcodedMenu(List<Map<String, Object>> menu) {
        // Sample menu items
        String[] items = {"Pizza", "Burger", "Pasta", "Salad", "Dessert", "Drink"};
        double[] prices = {12.99, 10.99, 11.99, 8.99, 5.99, 2.99};

        for (int i = 0; i < items.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("name", items[i]);
            item.put("price", prices[i]);
            item.put("available", true);
            menu.add(item);
        }
    }

}
