package com.bytebite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Order Service
 * Handles order creation, retrieval, and status updates
 */
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private DataSource dataSource;

    public OrderService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Object> createOrder(int userId, double totalAmount, String address) {
        Map<String, Object> result = new HashMap<>();
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO bytebite.orders (user_id, order_number, status, total_amount, shipping_address, order_date) " +
                        "VALUES (?, ?, 'PENDING', ?, ?, NOW()) RETURNING order_id";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, orderNumber);
                pstmt.setDouble(3, totalAmount);
                pstmt.setString(4, address);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        result.put("success", true);
                        result.put("orderId", orderId);
                        result.put("orderNumber", orderNumber);
                        logger.info("Order created: {} (ID: {})", orderNumber, orderId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error creating order", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getOrder(int orderId) {
        Map<String, Object> order = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT order_id, order_number, status, total_amount, order_date FROM bytebite.orders WHERE order_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        order.put("orderId", rs.getInt("order_id"));
                        order.put("orderNumber", rs.getString("order_number"));
                        order.put("status", rs.getString("status"));
                        order.put("totalAmount", rs.getDouble("total_amount"));
                        order.put("orderDate", rs.getTimestamp("order_date").toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving order", e);
        }

        return order;
    }

    public List<Map<String, Object>> getUserOrders(int userId) {
        List<Map<String, Object>> orders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT order_id, order_number, status, total_amount, order_date FROM bytebite.orders WHERE user_id = ? ORDER BY order_date DESC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> order = new HashMap<>();
                        order.put("orderId", rs.getInt("order_id"));
                        order.put("orderNumber", rs.getString("order_number"));
                        order.put("status", rs.getString("status"));
                        order.put("totalAmount", rs.getDouble("total_amount"));
                        order.put("orderDate", rs.getTimestamp("order_date").toString());
                        orders.add(order);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving user orders", e);
        }

        return orders;
    }

    public boolean updateOrderStatus(int orderId, String status) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE bytebite.orders SET status = ?, last_updated = NOW() WHERE order_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, orderId);
                
                return pstmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Error updating order status", e);
            return false;
        }
    }
}
