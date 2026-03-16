package com.hutech.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

import org.springframework.stereotype.Component;

/**
 * Buffer HTML response for GET / and write with Content-Length
 * to avoid ERR_INCOMPLETE_CHUNKED_ENCODING in the browser.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ResponseBufferFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only buffer GET requests to the home page
        return !"GET".equalsIgnoreCase(request.getMethod())
                || !"/".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
        } finally {
            wrapper.copyBodyToResponse();
        }
    }
}
