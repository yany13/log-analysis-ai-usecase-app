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

/**
 * KDS (Kitchen Display System) Servlet
 * Manages order display and status updates for kitchen staff
 */
@WebServlet(urlPatterns = {"/kds", "/api/kds"}, asyncSupported = true)
public class KDSServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(KDSServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Object>> activeOrders = new HashMap<>();

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

                updateOrderStatus(orderId, status);

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
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(getKDSPageHTML(request));
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

    private String getKDSPageHTML(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>ByteBite KDS</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; background: #1a1a1a; color: white; padding: 0; margin: 0; }\n");
        html.append(".kds-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; padding: 20px; }\n");
        html.append(".order-ticket { background: #2a2a2a; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; }\n");
        html.append(".order-number { font-size: 24px; font-weight: bold; color: #667eea; margin-bottom: 10px; }\n");
        html.append(".order-status { font-size: 14px; color: #999; margin-bottom: 15px; }\n");
        html.append(".order-items { margin: 15px 0; }\n");
        html.append(".order-item { padding: 8px; background: #1a1a1a; margin: 5px 0; border-radius: 4px; }\n");
        html.append(".status-button { padding: 10px; margin-top: 10px; background: #667eea; color: white; border: none; border-radius: 4px; cursor: pointer; width: 100%; }\n");
        html.append(".status-button:hover { background: #764ba2; }\n");
        html.append(".stats { position: fixed; top: 20px; right: 20px; background: #2a2a2a; padding: 20px; border-radius: 8px; }\n");
        html.append(".stat { margin: 10px 0; }\n");
        html.append(".stat-label { color: #999; font-size: 12px; }\n");
        html.append(".stat-value { font-size: 24px; font-weight: bold; color: #667eea; }\n");
        html.append("</style>\n");
        html.append("<script>\n");
        html.append("function updateStatus(orderId, newStatus) {\n");
        html.append("  fetch('").append(request.getContextPath()).append("/api/kds', {\n");
        html.append("    method: 'POST',\n");
        html.append("    headers: {'Content-Type': 'application/json'},\n");
        html.append("    body: JSON.stringify({orderId: orderId, status: newStatus})\n");
        html.append("  }).then(() => location.reload());\n");
        html.append("}\n");
        html.append("</script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='stats'>\n");
        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Total Orders</div>\n");
        html.append("<div class='stat-value'>").append(activeOrders.size()).append("</div>\n");
        html.append("</div>\n");
        html.append("<div class='stat'>\n");
        html.append("<div class='stat-label'>Ready</div>\n");
        html.append("<div class='stat-value'>0</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("<div class='kds-container'>\n");
        html.append("<div class='order-ticket'>\n");
        html.append("<div class='order-number'>ORD-001</div>\n");
        html.append("<div class='order-status'>Status: NEW</div>\n");
        html.append("<div class='order-items'>\n");
        html.append("<div class='order-item'>2x Pizza</div>\n");
        html.append("<div class='order-item'>1x Pasta</div>\n");
        html.append("</div>\n");
        html.append("<button class='status-button' onclick=\"updateStatus('1', 'IN_PROGRESS')\">Mark In Progress</button>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
