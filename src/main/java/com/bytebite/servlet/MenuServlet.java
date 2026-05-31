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
        String path = request.getServletPath();

        if (path.contains("api")) {
            handleAPIRequest(request, response);
        } else {
            handleWebRequest(request, response);
        }
    }

    private void handleWebRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/menu.html").forward(request, response);
    }

    private void handleAPIRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        List<Map<String, Object>> menu = getMenu();
        response.getWriter().write(objectMapper.writeValueAsString(menu));
    }

    private synchronized List<Map<String, Object>> getMenu() {
        long now = System.currentTimeMillis();
        
        // Check if cache is valid
        if (menuCache != null && (now - cacheTime) < cacheTTL) {
            logger.debug("Returning cached menu");
            return menuCache;
        }

        logger.info("Fetching menu from database");
        menuCache = buildMenu();
        cacheTime = now;
        return menuCache;
    }

    private List<Map<String, Object>> buildMenu() {
        List<Map<String, Object>> menu = new ArrayList<>();

        // Check if failure injection is requested
        if (FailureInjectionUtil.isGloballyEnabled()) {
            logger.info("MenuServlet: Attempting to load menu from database (potential connection leak scenario)");
            try {
                // This simulates a connection leak - we don't properly close the connection
                javax.naming.Context ctx = new javax.naming.InitialContext();
                javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

                // INTENTIONAL BUG: Connection not closed - simulating leak
                // In real scenario, repeated calls exhaust the connection pool
                Connection conn = ds.getConnection();
                logger.warn("⚠️ Connection leak scenario: Got connection {} from pool but NOT closing it",
                           System.identityHashCode(conn));

                // Intentionally leak the connection to the pool by not closing it
                String sql = "SELECT id, name, price FROM bytebite.menu_items LIMIT 100";
                try (java.sql.Statement stmt = conn.createStatement();
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
                // NOTE: Connection is intentionally NOT closed here - this causes the pool leak
                logger.error("❌ CONNECTION LEAK: Statement closed but Connection kept open, returning to client");
            } catch (Exception e) {
                logger.error("Database error in menu load (connection leak scenario)", e);
                // Fall back to hardcoded menu if database fails
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
