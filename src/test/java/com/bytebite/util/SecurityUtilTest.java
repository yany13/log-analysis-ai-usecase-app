package com.bytebite.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * SecurityUtil Tests
 */
public class SecurityUtilTest {

    @Test
    public void testPasswordHashing() {
        String password = "TestPassword123!";
        String hash1 = SecurityUtil.hashPassword(password);
        String hash2 = SecurityUtil.hashPassword(password);
        
        // Same password should produce same hash
        assertEquals(hash1, hash2);
        assertNotNull(hash1);
    }

    @Test
    public void testPasswordVerification() {
        String password = "TestPassword123!";
        String hash = SecurityUtil.hashPassword(password);
        
        assertTrue(SecurityUtil.verifyPassword(password, hash));
        assertFalse(SecurityUtil.verifyPassword("WrongPassword", hash));
    }

    @Test
    public void testSessionIdGeneration() {
        String sessionId1 = SecurityUtil.generateSessionId();
        String sessionId2 = SecurityUtil.generateSessionId();
        
        assertNotNull(sessionId1);
        assertNotNull(sessionId2);
        assertNotEquals(sessionId1, sessionId2);
        assertTrue(sessionId1.length() > 20);
    }

    @Test
    public void testInputSanitization() {
        String input = "<script>alert('XSS')</script>";
        String sanitized = SecurityUtil.sanitizeInput(input);
        
        assertFalse(sanitized.contains("<"));
        assertFalse(sanitized.contains(">"));
        assertTrue(sanitized.contains("&lt;"));
        assertTrue(sanitized.contains("&gt;"));
    }

    @Test
    public void testEmailValidation() {
        assertTrue(SecurityUtil.isValidEmail("user@example.com"));
        assertTrue(SecurityUtil.isValidEmail("test.user+tag@domain.co.uk"));
        assertFalse(SecurityUtil.isValidEmail("invalid.email"));
        assertFalse(SecurityUtil.isValidEmail(""));
        assertFalse(SecurityUtil.isValidEmail(null));
    }

    @Test
    public void testUsernameValidation() {
        assertTrue(SecurityUtil.isValidUsername("user123"));
        assertTrue(SecurityUtil.isValidUsername("test_user"));
        assertFalse(SecurityUtil.isValidUsername("ab")); // Too short
        assertFalse(SecurityUtil.isValidUsername("user@name")); // Invalid character
        assertFalse(SecurityUtil.isValidUsername(""));
    }

    @Test
    public void testPasswordStrength() {
        assertTrue(SecurityUtil.isStrongPassword("TestPassword123!"));
        assertTrue(SecurityUtil.isStrongPassword("SecureP@ss99"));
        assertFalse(SecurityUtil.isStrongPassword("weak")); // Too short, no numbers/special
        assertFalse(SecurityUtil.isStrongPassword("NoNumber!")); // No numbers
        assertFalse(SecurityUtil.isStrongPassword("Nospecial123")); // No special chars
    }

    @Test
    public void testSqlEscape() {
        String input = "O'Reilly";
        String escaped = SecurityUtil.escapeSqlString(input);
        
        assertEquals("O''Reilly", escaped);
    }

    @Test
    public void testCsrfToken() {
        String token1 = SecurityUtil.generateCsrfToken();
        String token2 = SecurityUtil.generateCsrfToken();
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(SecurityUtil.validateCsrfToken(token1, token1));
        assertFalse(SecurityUtil.validateCsrfToken(token1, token2));
    }
}
