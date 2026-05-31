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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KDS (Kitchen Display System) Servlet
 * Manages order display and status updates for kitchen staff
 */
public class KDSServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(KDSServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Object>> activeOrders = new HashMap<>();

    // Locks for deadlock scenario
    private final Object orderLock = new Object();
    private final Object inventoryLock = new Object();

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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String contentType = request.getContentType();

            if (contentType != null && contentType.contains("application/json")) {
                String body = new String(request.getInputStream().readAllBytes());
                Map<String, Object> update = objectMapper.readValue(body, Map.class);

                String orderId = (String) update.get("orderId");
                String status = (String) update.get("status");

                // Check for deadlock injection
                if (FailureInjectionUtil.isFailureType(request, FailureInjectionUtil.FAILURE_DEADLOCK)) {
                    FailureInjectionUtil.logFailureActivation(FailureInjectionUtil.FAILURE_DEADLOCK);
                    logger.warn("⚠️ DEADLOCK FAILURE INJECTION: Creating deadlock scenario");
                    updateOrderStatusWithDeadlock(orderId, status);
                } else {
                    updateOrderStatus(orderId, status);
                }

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"status\": \"updated\"}");

                logger.info("Order {} status updated to: {}", orderId, status);
            }
        } catch (Exception e) {
            logger.error("Error updating order", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleWebRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/kds.html").forward(request, response);
    }

    private void handleAPIRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String action = request.getParameter("action");

        if ("orders".equals(action)) {
            response.getWriter().write(objectMapper.writeValueAsString(activeOrders.values()));
        } else if ("stats".equals(action)) {
            Map<String, Object> stats = getKDSStats();
            response.getWriter().write(objectMapper.writeValueAsString(stats));
        } else {
            response.getWriter().write("{\"orders\": []}");
        }
    }

    private void updateOrderStatus(String orderId, String status) {
        try {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

            try (java.sql.Connection conn = ds.getConnection()) {
                String sql = "UPDATE bytebite.kds_orders SET kds_status = ?, updated_at = NOW() WHERE order_id = ?";
                
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, status);
                    pstmt.setString(2, orderId);
                    pstmt.executeUpdate();
                }

                // Update local cache
                if (activeOrders.containsKey(orderId)) {
                    activeOrders.get(orderId).put("status", status);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating order status", e);
        }
    }

    private Map<String, Object> getKDSStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", activeOrders.size());
        stats.put("newOrders", activeOrders.values().stream()
            .filter(o -> "NEW".equals(o.get("status")))
            .count());
        stats.put("inProgress", activeOrders.values().stream()
            .filter(o -> "IN_PROGRESS".equals(o.get("status")))
            .count());
        stats.put("ready", activeOrders.values().stream()
            .filter(o -> "READY".equals(o.get("status")))
            .count());
        return stats;
    }

    /**
     * Simulates a deadlock scenario with inverted lock ordering
     * Thread 1: locks orderLock, then tries to lock inventoryLock
     * Thread 2: locks inventoryLock, then tries to lock orderLock
     * Result: Both threads block indefinitely
     */
    private void updateOrderStatusWithDeadlock(String orderId, String status) {
        logger.warn("⚠️ Creating deadlock with order {} and status {}", orderId, status);

        // Thread 1: Lock orders first, then inventory
        Thread thread1 = new Thread(() -> {
            try {
                logger.info("Thread-1: Attempting to acquire orderLock");
                synchronized (orderLock) {
                    logger.warn("Thread-1: Acquired orderLock");
                    Thread.sleep(100); // Give thread2 time to acquire inventoryLock

                    logger.info("Thread-1: Attempting to acquire inventoryLock");
                    synchronized (inventoryLock) {
                        logger.info("Thread-1: Acquired inventoryLock");
                        logger.info("Thread-1: Updating order {}", orderId);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Thread 2: Lock inventory first, then orders (inverted order = deadlock)
        Thread thread2 = new Thread(() -> {
            try {
                logger.info("Thread-2: Attempting to acquire inventoryLock");
                synchronized (inventoryLock) {
                    logger.warn("Thread-2: Acquired inventoryLock");
                    Thread.sleep(100); // Give thread1 time to acquire orderLock

                    logger.info("Thread-2: Attempting to acquire orderLock");
                    synchronized (orderLock) {
                        logger.info("Thread-2: Acquired orderLock");
                        logger.info("Thread-2: Updating inventory");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.setName("OrderUpdateWorker-1");
        thread2.setName("InventoryUpdateWorker-2");

        thread1.start();
        thread2.start();

        // Wait briefly for deadlock to establish
        try {
            Thread.sleep(500);
            if (thread1.isAlive() && thread2.isAlive()) {
                logger.error("❌ DEADLOCK SCENARIO: Both threads are now BLOCKED");
                logger.error("Thread-1 state: {} (waiting for inventoryLock)", thread1.getState());
                logger.error("Thread-2 state: {} (waiting for orderLock)", thread2.getState());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
