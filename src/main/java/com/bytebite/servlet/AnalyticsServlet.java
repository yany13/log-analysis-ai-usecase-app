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
import java.util.*;
import java.util.concurrent.*;

/**
 * Analytics Servlet
 * Handles event tracking and analytics with batch processing
 */
@WebServlet(urlPatterns = {"/analytics", "/api/analytics"}, asyncSupported = true)
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
        String path = request.getServletPath();

        if (path.contains("api")) {
            handleAPIAnalytics(request, response);
        } else {
            handleWebAnalytics(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String contentType = request.getContentType();

            if (contentType != null && contentType.contains("application/json")) {
                String body = new String(request.getInputStream().readAllBytes());
                Map<String, Object> event = objectMapper.readValue(body, Map.class);

                // Add event to queue
                event.put("timestamp", System.currentTimeMillis());
                event.put("user", request.getSession().getAttribute("user"));
                event.put("ip", request.getRemoteAddr());

                eventQueue.offer(event);

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                response.getWriter().write("{\"status\": \"received\"}");

                logger.debug("Event queued: {}", event.get("eventType"));
            }
        } catch (Exception e) {
            logger.error("Error processing analytics event", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleAPIAnalytics(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("eventQueueSize", eventQueue.size());
        analytics.put("batchSize", batchSize);
        analytics.put("timeout", batchTimeoutSeconds);

        response.getWriter().write(objectMapper.writeValueAsString(analytics));
    }

    private void handleWebAnalytics(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(getAnalyticsPageHTML(request));
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

    private String getAnalyticsPageHTML(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>ByteBite Analytics</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("h1 { text-align: center; color: #333; }\n");
        html.append(".stats { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; margin-top: 40px; }\n");
        html.append(".stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); text-align: center; }\n");
        html.append(".stat-value { font-size: 32px; font-weight: bold; color: #667eea; }\n");
        html.append(".stat-label { color: #999; margin-top: 10px; }\n");
        html.append(".nav { margin-bottom: 20px; }\n");
        html.append(".nav a { margin-right: 15px; text-decoration: none; color: #667eea; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='container'>\n");
        html.append("<div class='nav'>\n");
        html.append("<a href='").append(request.getContextPath()).append("/menu'>Menu</a>\n");
        html.append("<a href='").append(request.getContextPath()).append("/logout'>Logout</a>\n");
        html.append("</div>\n");
        html.append("<h1>Analytics Dashboard</h1>\n");
        html.append("<div class='stats'>\n");
        html.append("<div class='stat-card'><div class='stat-value'>").append(eventQueue.size()).append("</div><div class='stat-label'>Queued Events</div></div>\n");
        html.append("<div class='stat-card'><div class='stat-value'>").append(batchSize).append("</div><div class='stat-label'>Batch Size</div></div>\n");
        html.append("<div class='stat-card'><div class='stat-value'>").append(batchTimeoutSeconds).append("s</div><div class='stat-label'>Batch Timeout</div></div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
