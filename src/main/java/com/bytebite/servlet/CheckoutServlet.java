package com.bytebite.servlet;

import com.bytebite.util.FailureInjectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout Servlet
 * Handles order creation and transaction processing
 */
public class CheckoutServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if (path.contains("api")) {
            handleGetOrders(request, response);
        } else {
            handleCheckoutForm(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            handleAPIOrder(request, response);
        } else {
            handleFormOrder(request, response);
        }
    }

    private void handleCheckoutForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/checkout.html").forward(request, response);
    }

    private void handleAPIOrder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            response.setContentType("application/json");

            // Check for SSL failure injection
            if (FailureInjectionUtil.isGloballyEnabled() && "true".equals(request.getParameter("injectSSLFailure"))) {
                logger.warn("⚠️ SSL FAILURE INJECTION: Attempting connection to self-signed HTTPS endpoint");
                try {
                    testSSLHandshakeFailure();
                    // If we got here, SSL didn't fail (shouldn't happen in real scenario)
                    logger.warn("SSL test endpoint did not fail as expected");
                } catch (SSLHandshakeException e) {
                    logger.error("❌ SSL HANDSHAKE FAILURE SCENARIO: {}", e.getMessage(), e);
                    response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                    Map<String, String> error = new HashMap<>();
                    error.put("status", "error");
                    error.put("message", "SSL Certificate Validation Failed: " + e.getMessage());
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                    return;
                }
            }

            // Parse request
            String body = new String(request.getInputStream().readAllBytes());
            Map<String, Object> orderData = objectMapper.readValue(body, Map.class);

            // Create order
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String user = (String) request.getSession().getAttribute("user");

            Map<String, Object> result = createOrder(user, orderData, orderNumber);

            if ("success".equals(result.get("status"))) {
                response.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

            response.getWriter().write(objectMapper.writeValueAsString(result));
            logger.info("Order created: {} by user: {}", orderNumber, user);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private void handleFormOrder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String itemCount = request.getParameter("itemCount");
            String totalAmount = request.getParameter("totalAmount");
            String user = (String) request.getSession().getAttribute("user");

            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            logger.info("Order submitted: {} by user: {} (items: {}, total: {})", 
                       orderNumber, user, itemCount, totalAmount);

            // Redirect to order confirmation
            response.sendRedirect(request.getContextPath() + "/checkout?orderId=" + orderNumber + "&status=success");
        } catch (Exception e) {
            logger.error("Error processing order", e);
            response.sendRedirect(request.getContextPath() + "/checkout?status=error");
        }
    }

    private void handleGetOrders(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String user = (String) request.getSession().getAttribute("user");

        try {
            Map<String, Object> orders = new HashMap<>();
            orders.put("user", user);
            orders.put("orders", new Object[]{});
            orders.put("total", 0);

            response.getWriter().write(objectMapper.writeValueAsString(orders));
        } catch (Exception e) {
            logger.error("Error retrieving orders", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> createOrder(String user, Map<String, Object> orderData, String orderNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Save order to database
            javax.naming.Context ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("java:comp/env/jdbc/bytebite");

            try (Connection conn = ds.getConnection()) {
                String sql = "INSERT INTO bytebite.orders (user_id, order_number, status, total_amount, order_date) " +
                            "VALUES (1, ?, 'PENDING', ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, orderNumber);
                    pstmt.setDouble(2, ((Number) orderData.getOrDefault("totalAmount", 0)).doubleValue());
                    pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    pstmt.executeUpdate();
                }
            }

            result.put("status", "success");
            result.put("orderNumber", orderNumber);
            result.put("message", "Order created successfully");
        } catch (Exception e) {
            logger.error("Error creating order in database", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Attempt to connect to an HTTPS endpoint with a self-signed certificate
     * This will fail with SSLHandshakeException because the cert is not in the truststore
     */
    private void testSSLHandshakeFailure() throws IOException {
        // Try to connect to a test HTTPS endpoint with self-signed cert
        // This endpoint doesn't actually need to exist - the SSL handshake will fail first
        String testUrl = "https://self-signed.example.com:8443/payment-gateway";

        logger.warn("Attempting HTTPS connection to: {}", testUrl);

        try {
            URL url = new URL(testUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Set timeout to avoid hanging
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            logger.info("Initiating SSL handshake with {}", url.getHost());
            // This will trigger the SSL handshake and fail
            conn.connect();

            logger.error("Unexpected success connecting to {}", url);
        } catch (SSLHandshakeException e) {
            // Expected failure - re-throw to caller
            throw e;
        } catch (javax.net.ssl.SSLException e) {
            // Re-wrap as SSLHandshakeException for clarity
            throw new SSLHandshakeException("SSL handshake failed: " + e.getMessage());
        }
    }

}
