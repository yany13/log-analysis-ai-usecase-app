package com.bytebite.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CORS (Cross-Origin Resource Sharing) Filter
 * Handles CORS headers for cross-origin requests
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class CORSFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CORSFilter.class);
    private String allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private String exposedHeaders;
    private String maxAge;
    private String allowCredentials;

    @Override
    public void init(FilterConfig config) throws ServletException {
        allowedOrigins = config.getInitParameter("allowedOrigins");
        allowedMethods = config.getInitParameter("allowedMethods");
        allowedHeaders = config.getInitParameter("allowedHeaders");
        exposedHeaders = config.getInitParameter("exposedHeaders");
        maxAge = config.getInitParameter("maxAge");
        allowCredentials = config.getInitParameter("allowCredentials");

        // Set defaults
        allowedOrigins = allowedOrigins != null ? allowedOrigins : "*";
        allowedMethods = allowedMethods != null ? allowedMethods : "GET,POST,PUT,DELETE,OPTIONS";
        allowedHeaders = allowedHeaders != null ? allowedHeaders : "Content-Type,Authorization";
        exposedHeaders = exposedHeaders != null ? exposedHeaders : "Authorization";
        maxAge = maxAge != null ? maxAge : "3600";
        allowCredentials = allowCredentials != null ? allowCredentials : "true";

        logger.info("CORSFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set CORS headers
        httpResponse.setHeader("Access-Control-Allow-Origin", allowedOrigins);
        httpResponse.setHeader("Access-Control-Allow-Methods", allowedMethods);
        httpResponse.setHeader("Access-Control-Allow-Headers", allowedHeaders);
        httpResponse.setHeader("Access-Control-Expose-Headers", exposedHeaders);
        httpResponse.setHeader("Access-Control-Max-Age", maxAge);
        httpResponse.setHeader("Access-Control-Allow-Credentials", allowCredentials);

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("CORSFilter destroyed");
    }
}
