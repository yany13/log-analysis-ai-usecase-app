package com.bytebite.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Request Logging Filter
 * Logs incoming HTTP requests and outgoing responses with timing information
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class RequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private String[] excludePatterns;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String excludeParam = config.getInitParameter("excludePatterns");
        if (excludeParam != null) {
            excludePatterns = excludeParam.split(",");
        } else {
            excludePatterns = new String[]{};
        }
        logger.info("RequestLoggingFilter initialized with {} exclude patterns", excludePatterns.length);
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

        long startTime = System.currentTimeMillis();
        String method = httpRequest.getMethod();
        String queryString = httpRequest.getQueryString();
        String fullPath = queryString != null ? requestPath + "?" + queryString : requestPath;

        logger.info("{} {} from {}", method, fullPath, httpRequest.getRemoteAddr());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            logger.info("{} {} - Status: {} - Duration: {}ms", method, requestPath, status, duration);
        }
    }

    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter destroyed");
    }

    private boolean shouldExclude(String path) {
        for (String pattern : excludePatterns) {
            if (Pattern.matches(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }
}
