package com.bytebite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * User Service
 * Handles user authentication and profile management
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private DataSource dataSource;

    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean authenticate(String username, String password) {
        try {
            // LDAP authentication would be implemented here
            // For now, simple in-memory authentication
            return "admin".equals(username) && "admin".equals(password);
        } catch (Exception e) {
            logger.error("Authentication failed", e);
            return false;
        }
    }

    public Map<String, Object> getUserProfile(String username) {
        Map<String, Object> profile = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT user_id, username, email, is_admin FROM bytebite.users WHERE username = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        profile.put("userId", rs.getInt("user_id"));
                        profile.put("username", rs.getString("username"));
                        profile.put("email", rs.getString("email"));
                        profile.put("isAdmin", rs.getBoolean("is_admin"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving user profile", e);
        }

        return profile;
    }

    public boolean createUser(String username, String email, String passwordHash) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO bytebite.users (username, email, password_hash, is_active) VALUES (?, ?, ?, true)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, passwordHash);
                
                return pstmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return false;
        }
    }

    public boolean updateLastLogin(String username) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE bytebite.users SET last_login = NOW() WHERE username = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                return pstmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.error("Error updating last login", e);
            return false;
        }
    }
}
