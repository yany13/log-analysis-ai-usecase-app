package com.bytebite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Analytics Service
 * Handles event tracking and analytics data persistence
 */
public class AnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private DataSource dataSource;
    private ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean trackEvent(String eventType, Map<String, Object> eventData, Integer userId, String sessionId, String ipAddress) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO bytebite.analytics_events (event_type, event_data, user_id, session_id, ip_address, created_at) " +
                        "VALUES (?, ?::jsonb, ?, ?, ?, NOW())";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, eventType);
                pstmt.setString(2, objectMapper.writeValueAsString(eventData));
                pstmt.setObject(3, userId);
                pstmt.setString(4, sessionId);
                pstmt.setString(5, ipAddress);
                
                return pstmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Error tracking event", e);
            return false;
        }
    }

    public Map<String, Object> getEventStats(long startTime, long endTime) {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT event_type, COUNT(*) as count FROM bytebite.analytics_events " +
                        "WHERE created_at >= to_timestamp(?) AND created_at <= to_timestamp(?) " +
                        "GROUP BY event_type";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, startTime / 1000.0);
                pstmt.setDouble(2, endTime / 1000.0);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        stats.put(rs.getString("event_type"), rs.getLong("count"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving event stats", e);
        }

        return stats;
    }

    public List<Map<String, Object>> getUserEvents(int userId, int limit) {
        List<Map<String, Object>> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT event_type, event_data, created_at FROM bytebite.analytics_events " +
                        "WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, limit);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("eventType", rs.getString("event_type"));
                        event.put("eventData", rs.getString("event_data"));
                        event.put("timestamp", rs.getTimestamp("created_at").toString());
                        events.add(event);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving user events", e);
        }

        return events;
    }

    public boolean logApplicationEvent(String level, String category, String message, String sourceClass, int lineNumber) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO bytebite.application_logs (level, category, message, source_class, line_number, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, level);
                pstmt.setString(2, category);
                pstmt.setString(3, message);
                pstmt.setString(4, sourceClass);
                pstmt.setInt(5, lineNumber);
                
                return pstmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Error logging application event", e);
            return false;
        }
    }

    public List<Map<String, Object>> getApplicationLogs(String level, int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT level, category, message, source_class, line_number, created_at FROM bytebite.application_logs " +
                        "WHERE level = ? ORDER BY created_at DESC LIMIT ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, level);
                pstmt.setInt(2, limit);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> log = new HashMap<>();
                        log.put("level", rs.getString("level"));
                        log.put("category", rs.getString("category"));
                        log.put("message", rs.getString("message"));
                        log.put("source", rs.getString("source_class") + ":" + rs.getInt("line_number"));
                        log.put("timestamp", rs.getTimestamp("created_at").toString());
                        logs.add(log);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving application logs", e);
        }

        return logs;
    }
}
