package com.bytebite.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * LDAP Authentication Filter
 * Verifies LDAP authentication for protected resources
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class LDAPAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LDAPAuthenticationFilter.class);
    private String[] excludePatterns;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String excludeParam = config.getInitParameter("excludePatterns");
        if (excludeParam != null) {
            excludePatterns = excludeParam.split(",");
        } else {
            excludePatterns = new String[]{};
        }
        logger.info("LDAPAuthenticationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestPath = httpRequest.getRequestURI();

        // Check if path should be excluded
        if (shouldExclude(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        String user = httpRequest.getRemoteUser();

        // Check if user is authenticated
        if (session == null || user == null) {
            logger.warn("Unauthenticated access attempt to: {}", requestPath);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        // Check session validity
        try {
            session.getCreationTime(); // This will throw IllegalStateException if session is invalidated
            chain.doFilter(request, response);
        } catch (IllegalStateException e) {
            logger.warn("Invalid session for user: {}", user);
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
        }
    }

    @Override
    public void destroy() {
        logger.info("LDAPAuthenticationFilter destroyed");
    }

    private boolean shouldExclude(String path) {
        for (String pattern : excludePatterns) {
            if (path.matches(pattern.trim().replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }
}
