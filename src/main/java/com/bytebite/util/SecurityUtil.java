package com.bytebite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * Security Utility
 * Provides security-related helper functions
 */
public class SecurityUtil {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);

    /**
     * Hash a password using SHA-256
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(messageDigest);
        } catch (Exception e) {
            logger.error("Error hashing password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify a password against its hash
     */
    public static boolean verifyPassword(String password, String hash) {
        try {
            String hashedInput = hashPassword(password);
            return hashedInput.equals(hash);
        } catch (Exception e) {
            logger.error("Error verifying password", e);
            return false;
        }
    }

    /**
     * Generate a random session ID
     */
    public static String generateSessionId() {
        try {
            byte[] randomBytes = new byte[32];
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            secureRandom.nextBytes(randomBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } catch (Exception e) {
            logger.error("Error generating session ID", e);
            throw new RuntimeException("Session ID generation failed", e);
        }
    }

    /**
     * Sanitize user input to prevent XSS attacks
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    /**
     * Check if a string is a valid email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Check if a string is a valid username
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // Username: 3-20 characters, alphanumeric and underscores only
        String usernameRegex = "^[a-zA-Z0-9_]{3,20}$";
        return username.matches(usernameRegex);
    }

    /**
     * Check if a password is strong enough
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        // Must contain uppercase, lowercase, number, and special character
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        return hasUppercase && hasLowercase && hasNumber && hasSpecial;
    }

    /**
     * Encode a string for SQL to prevent injection
     */
    public static String escapeSqlString(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''");
    }

    /**
     * Validate CSRF token
     */
    public static boolean validateCsrfToken(String token, String expected) {
        if (token == null || expected == null) {
            return false;
        }
        return token.equals(expected);
    }

    /**
     * Generate a CSRF token
     */
    public static String generateCsrfToken() {
        return generateSessionId();
    }
}
