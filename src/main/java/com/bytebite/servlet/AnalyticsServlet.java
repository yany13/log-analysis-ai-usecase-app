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
import java.util.concurrent.*;

/**
 * Analytics Servlet
 * Handles event tracking and analytics with batch processing
 */
public class AnalyticsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private BlockingQueue<Map<String, Object>> eventQueue;
    private ExecutorService executor;
    private int batchSize;
    private int batchTimeoutSeconds;

    @Override
    public void init() throws ServletException {
        super.init();
        String batchSizeParam = getInitParameter("batch.size");
        String batchTimeoutParam = getInitParameter("batch.timeout.seconds");

        batchSize = (batchSizeParam != null ? Integer.parseInt(batchSizeParam) : 100);
        batchTimeoutSeconds = (batchTimeoutParam != null ? Integer.parseInt(batchTimeoutParam) : 60);

        eventQueue = new LinkedBlockingQueue<>();
        executor = Executors.newSingleThreadExecutor();

        // Start batch processor thread
        executor.execute(this::processBatches);

        logger.info("AnalyticsServlet initialized with batch size: {}, timeout: {} seconds",
                   batchSize, batchTimeoutSeconds);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/analytics.html").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            String body = new String(request.getInputStream().readAllBytes());
            Map<String, Object> payload = objectMapper.readValue(body, Map.class);

            // Check for all-time report request (OOM failure scenario)
            String action = (String) payload.get("action");
            String failureType = (String) payload.get("failureType");
            if ("all-time-report".equals(action) && FailureInjectionUtil.FAILURE_OOM.equals(failureType)) {
                FailureInjectionUtil.logFailureActivation(FailureInjectionUtil.FAILURE_OOM);
                logger.warn("⚠️ OOM FAILURE INJECTION: Loading all-time analytics without pagination");
                try {
                    generateAllTimeReport(response);
                    return;
                } catch (OutOfMemoryError e) {
                    logger.error("❌ OOM FAILURE SCENARIO TRIGGERED: {}", e.getMessage(), e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"OutOfMemoryError: Java heap space\"}");
                    return;
                }
            }

            // Add event to queue
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("user", request.getSession().getAttribute("user"));
            payload.put("ip", request.getRemoteAddr());

            eventQueue.offer(payload);

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            response.getWriter().write("{\"status\": \"received\"}");

            logger.debug("Event queued: {}", payload.get("eventType"));
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"JSON body required\"}");
        }
    }

    private void processBatches() {
        List<Map<String, Object>> batch = new ArrayList<>();
        long lastProcessTime = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                long now = System.currentTimeMillis();
                long timeSinceLastProcess = now - lastProcessTime;

                // Wait for event with timeout
                Map<String, Object> event = eventQueue.poll(
                    Math.max(1, batchTimeoutSeconds * 1000 - timeSinceLastProcess),
                    TimeUnit.MILLISECONDS
                );

                if (event != null) {
                    batch.add(event);
                }

                // Process batch if full or timeout reached
                if (batch.size() >= batchSize || 
                    (timeSinceLastProcess >= batchTimeoutSeconds * 1000 && !batch.isEmpty())) {
                    processBatch(batch);
                    batch.clear();
                    lastProcessTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Analytics batch processor interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error processing analytics batch", e);
            }
        }
    }

    private void processBatch(List<Map<String, Object>> batch) {
        logger.info("Processing analytics batch with {} events", batch.size());

        try {
            // Save batch to database
            javax.naming.Context ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

            for (Map<String, Object> event : batch) {
                try (java.sql.Connection conn = ds.getConnection()) {
                    String sql = "INSERT INTO bytebite.analytics_events (event_type, event_data, user_id, created_at) " +
                               "VALUES (?, ?::jsonb, 1, NOW())";
                    
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, (String) event.get("eventType"));
                        pstmt.setString(2, objectMapper.writeValueAsString(event));
                        pstmt.executeUpdate();
                    }
                }
            }

            logger.info("Batch processed successfully");
        } catch (Exception e) {
            logger.error("Error saving analytics batch", e);
        }
    }

    private void generateAllTimeReport(HttpServletResponse response) throws IOException {
        logger.info("Generating all-time analytics report - loading ALL data into memory");

        // Global, unbounded list - intentionally causes OOM
        List<Map<String, Object>> allEventsUnmanaged = new ArrayList<>();

        try {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

            try (Connection conn = ds.getConnection()) {
                // NO LIMIT clause - loads ALL events ever created
                String sql = "SELECT event_type, event_data, user_id, created_at FROM bytebite.analytics_events";
                logger.warn("Executing unbounded query: {}", sql);

                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(sql)) {

                    int count = 0;
                    while (rs.next()) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("eventType", rs.getString("event_type"));
                        event.put("eventData", rs.getString("event_data"));
                        event.put("userId", rs.getLong("user_id"));
                        event.put("createdAt", rs.getTimestamp("created_at"));

                        // Add duplicate data to accelerate OOM (simulate large payloads)
                        byte[] largePayload = new byte[1024 * 100]; // 100KB per event
                        event.put("payload", largePayload);

                        allEventsUnmanaged.add(event);
                        count++;

                        if (count % 1000 == 0) {
                            logger.warn("⚠️ Loaded {} events into memory (heap usage increasing)", count);
                        }
                    }

                    logger.error("❌ Successfully loaded {} events into unbounded memory structure", count);
                    // Events remain in memory - never released, causing heap exhaustion on next large report
                }
            }

            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(
                Collections.singletonMap("totalEvents", allEventsUnmanaged.size())));

        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error generating all-time report", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        logger.info("AnalyticsServlet destroyed");
    }

}
