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
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            logger.warn("Login attempt with missing credentials from {}", request.getRemoteAddr());
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_credentials");
            return;
        }

        // Attempt LDAP authentication
        if (authenticateUser(username, password, request)) {
            logger.info("User {} authenticated successfully from {}", username, request.getRemoteAddr());

            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("user", username);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            // Redirect to menu
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

    private boolean authenticateUser(String username, String password, HttpServletRequest request) {
        try {
            boolean injectLdapFailure = FailureInjectionUtil.isFailureType(request, FailureInjectionUtil.FAILURE_LDAP_TIMEOUT);

            if (injectLdapFailure) {
                FailureInjectionUtil.logFailureActivation(FailureInjectionUtil.FAILURE_LDAP_TIMEOUT);
                logger.warn("⚠️ LDAP Failure Injection: Simulating connection timeout");
                // Simulate LDAP timeout by throwing exception
                throw new ServletException("LDAP connection timeout (failure injection)");
            }

            logger.info("Attempting LDAP authentication for user: {} via JNDI Realm", username);

            // Use Tomcat's container-managed authentication via Realm
            request.login(username, password);

            logger.info("LDAP authentication successful for user: {}", username);
            return true;

        } catch (ServletException e) {
            logger.error("LDAP authentication failed for user: {} - Error: {}", username, e.getMessage(), e);
            if (FailureInjectionUtil.isFailureType(request, FailureInjectionUtil.FAILURE_LDAP_TIMEOUT)) {
                logger.error("❌ LDAP FAILURE SCENARIO: {}", e.getClass().getSimpleName());
            }
            return false;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return false;
        }
    }

}
