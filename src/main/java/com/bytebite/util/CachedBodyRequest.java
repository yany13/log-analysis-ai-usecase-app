package com.bytebite.util;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Caches HTTP request body to allow multiple reads
 * Solves the issue where request.getInputStream() can only be read once
 */
public class CachedBodyRequest extends HttpServletRequestWrapper {
    private byte[] cachedBody;

    public CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        // Read and cache the body from the original request
        cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            private final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener listener) {
                // Not implemented for this use case
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /**
     * Get cached body as byte array
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * Get cached body as string
     */
    public String getCachedBodyAsString() {
        return new String(cachedBody);
    }
}
