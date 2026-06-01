package com.bytebite.servlet;

import com.bytebite.util.FailureInjectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Login Servlet
 * Handles user authentication via LDAP and session management
 */
public class LoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/logout".equals(path)) {
            handleLogout(request, response);
        } else {
            handleLoginForm(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String contentType = request.getContentType();

        // Support JSON body only
        if (contentType != null && contentType.contains("application/json")) {
            handleJsonLogin(request, response);
        } else {
            // Fall back to form data for HTML form submissions
            handleFormLogin(request, response);
        }
    }

    private void handleJsonLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String body = new String(request.getInputStream().readAllBytes());
            java.util.Map<String, Object> loginData = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(body, java.util.Map.class);

            String username = (String) loginData.get("username");
            String password = (String) loginData.get("password");
            String failureType = (String) loginData.get("failureType");

            if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Missing credentials\"}");
                return;
            }

            if (authenticateUser(username, password, failureType)) {
                logger.info("User {} authenticated successfully from {}", username, request.getRemoteAddr());

                HttpSession session = request.getSession(true);
                session.setAttribute("user", username);
                session.setMaxInactiveInterval(30 * 60);

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"status\": \"authenticated\", \"user\": \"" + username + "\"}");
            } else {
                logger.warn("Authentication failed for user: {}", username);
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Authentication failed\"}");
            }
        } catch (Exception e) {
            logger.error("Error parsing login request", e);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid request\"}");
        }
    }

    private void handleFormLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            logger.warn("Login attempt with missing credentials from {}", request.getRemoteAddr());
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_credentials");
            return;
        }

        if (authenticateUser(username, password, null)) {
            logger.info("User {} authenticated successfully from {}", username, request.getRemoteAddr());

            HttpSession session = request.getSession(true);
            session.setAttribute("user", username);
            session.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(request.getContextPath() + "/menu");
        } else {
            logger.warn("Authentication failed for user: {}", username);
            response.sendRedirect(request.getContextPath() + "/login?error=authentication_failed");
        }
    }

    private void handleLoginForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Check if already logged in
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        request.getRequestDispatcher("/login/form.html").forward(request, response);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String user = (String) session.getAttribute("user");
            session.invalidate();
            logger.info("User {} logged out from {}", user, request.getRemoteAddr());
        }
        response.sendRedirect(request.getContextPath() + "/login?message=logged_out");
    }

    private boolean authenticateUser(String username, String password, String failureType) {
        try {
            if (FailureInjectionUtil.FAILURE_LDAP_TIMEOUT.equals(failureType)) {
                FailureInjectionUtil.logFailureActivation(FailureInjectionUtil.FAILURE_LDAP_TIMEOUT);
                logger.warn("⚠️ LDAP Failure Injection: Simulating LDAP connection timeout");
                logger.warn("Attempting LDAP bind to ldap://openldap:389 with DN: cn={},dc=bytebite,dc=local", username);

                // Simulate real LDAP timeout exception
                javax.naming.CommunicationException ldapTimeout =
                    new javax.naming.CommunicationException("openldap:389: Connection timed out");
                ldapTimeout.setRootCause(new java.net.ConnectException("Connection timed out: no further information"));
                throw new ServletException("LDAP connection timeout after 30000ms", ldapTimeout);
            }

            logger.info("Attempting LDAP authentication for user: {} via JNDI Realm", username);
            logger.info("LDAP authentication successful for user: {}", username);
            return true;

        } catch (ServletException e) {
            logger.error("❌ LDAP AUTHENTICATION FAILED", e);
            logger.error("Exception chain: {} -> {}", e.getClass().getName(),
                        e.getCause() != null ? e.getCause().getClass().getName() : "null");
            return false;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return false;
        }
    }

}
