package com.bytebite.filter;

import com.bytebite.util.CachedBodyRequest;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter that caches request body to allow multiple reads
 * Must be applied before any servlet that needs to read the body
 */
public class CachedBodyFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CachedBodyFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CachedBodyFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String contentType = httpRequest.getContentType();

            // Only cache JSON requests to avoid overhead
            if (contentType != null && contentType.contains("application/json")) {
                try {
                    request = new CachedBodyRequest(httpRequest);
                } catch (IOException e) {
                    logger.warn("Failed to cache request body", e);
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
