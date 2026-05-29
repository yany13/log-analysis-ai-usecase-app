package com.bytebite.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Login Servlet
 * Handles user authentication via LDAP and session management
 */
@WebServlet(urlPatterns = {"/login", "/logout"}, asyncSupported = true)
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

        // Display login form
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(getLoginFormHTML(request));
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
            // LDAP authentication would be implemented here
            // For now, accept admin/admin for testing
            return "admin".equals(username) && "admin".equals(password);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return false;
        }
    }

    private String getLoginFormHTML(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String errorMessage = request.getParameter("error");
        String message = request.getParameter("message");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>ByteBite Login</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }\n");
        html.append(".login-container { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: white; padding: 40px; border-radius: 10px; box-shadow: 0 10px 25px rgba(0,0,0,0.2); width: 300px; }\n");
        html.append("h1 { text-align: center; color: #333; }\n");
        html.append("form { display: flex; flex-direction: column; }\n");
        html.append("input { padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 5px; }\n");
        html.append("button { padding: 10px; margin: 20px 0; background: #667eea; color: white; border: none; border-radius: 5px; cursor: pointer; }\n");
        html.append("button:hover { background: #764ba2; }\n");
        html.append(".error { color: red; text-align: center; margin: 10px 0; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='login-container'>\n");
        html.append("<h1>ByteBite</h1>\n");
        
        if ("logged_out".equals(message)) {
            html.append("<p style='text-align:center; color: green;'>You have been logged out successfully</p>\n");
        }
        
        if ("authentication_failed".equals(errorMessage)) {
            html.append("<p class='error'>Invalid username or password</p>\n");
        } else if ("invalid_credentials".equals(errorMessage)) {
            html.append("<p class='error'>Please enter both username and password</p>\n");
        }
        
        html.append("<form method='POST' action='").append(contextPath).append("/login'>\n");
        html.append("<input type='text' name='username' placeholder='Username' required>\n");
        html.append("<input type='password' name='password' placeholder='Password' required>\n");
        html.append("<button type='submit'>Login</button>\n");
        html.append("</form>\n");
        html.append("<p style='text-align: center; font-size: 12px;'>Test: admin/admin</p>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
