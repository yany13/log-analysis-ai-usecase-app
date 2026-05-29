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
@WebServlet(urlPatterns = {"/checkout", "/api/orders"}, asyncSupported = true)
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
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(getCheckoutFormHTML(request));
    }

    private void handleAPIOrder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            response.setContentType("application/json");

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

    private String getCheckoutFormHTML(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>ByteBite Checkout</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px; }\n");
        html.append(".checkout-container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n");
        html.append("h1 { text-align: center; color: #333; }\n");
        html.append("form { display: flex; flex-direction: column; }\n");
        html.append("label { margin-top: 15px; color: #333; font-weight: bold; }\n");
        html.append("input, textarea { padding: 10px; margin-top: 5px; border: 1px solid #ddd; border-radius: 4px; }\n");
        html.append("button { padding: 12px; margin-top: 20px; background: #667eea; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }\n");
        html.append("button:hover { background: #764ba2; }\n");
        html.append(".nav { margin-bottom: 20px; }\n");
        html.append(".nav a { text-decoration: none; color: #667eea; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='checkout-container'>\n");
        html.append("<div class='nav'><a href='").append(request.getContextPath()).append("/menu'>← Back to Menu</a></div>\n");
        html.append("<h1>Checkout</h1>\n");
        html.append("<form method='POST' action='").append(request.getContextPath()).append("/checkout'>\n");
        html.append("<label for='itemCount'>Number of Items:</label>\n");
        html.append("<input type='number' id='itemCount' name='itemCount' required>\n");
        html.append("<label for='totalAmount'>Total Amount ($):</label>\n");
        html.append("<input type='number' id='totalAmount' name='totalAmount' step='0.01' required>\n");
        html.append("<label for='address'>Delivery Address:</label>\n");
        html.append("<textarea id='address' name='address' rows='4' required></textarea>\n");
        html.append("<button type='submit'>Place Order</button>\n");
        html.append("</form>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
