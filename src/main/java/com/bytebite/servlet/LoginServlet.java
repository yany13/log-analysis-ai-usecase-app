package com.bytebite.servlet;

import com.bytebite.config.LDAPConfig;
import com.bytebite.util.FailureInjectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.util.Hashtable;

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
        if (authenticateUser(username, password)) {
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

    private boolean authenticateUser(String username, String password) {
        try {
            boolean injectFailure = FailureInjectionUtil.isGloballyEnabled();

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            if (injectFailure) {
                logger.warn("⚠️ LDAP Failure Injection: Using invalid connection timeout");
                // Inject failure: use impossibly short timeout
                env.put("com.sun.jndi.ldap.connect.timeout", "1");
                env.put(Context.PROVIDER_URL, LDAPConfig.getLdapUrl());
            } else {
                env.put("com.sun.jndi.ldap.connect.timeout", "5000");
                env.put(Context.PROVIDER_URL, LDAPConfig.getLdapUrl());
            }

            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, String.format("uid=%s,ou=people,%s", username, LDAPConfig.getBaseDn()));
            env.put(Context.SECURITY_CREDENTIALS, password);

            logger.info("Attempting LDAP authentication for user: {}", username);

            // This will timeout if failure injection is enabled
            InitialDirContext ctx = new InitialDirContext(env);
            ctx.close();

            logger.info("LDAP authentication successful for user: {}", username);
            return true;

        } catch (NamingException e) {
            logger.error("LDAP authentication failed for user: {} - Error: {}", username, e.getMessage(), e);
            if (FailureInjectionUtil.isGloballyEnabled()) {
                logger.error("❌ LDAP FAILURE SCENARIO: {}", e.getClass().getSimpleName());
            }
            return false;
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return false;
        }
    }

}
