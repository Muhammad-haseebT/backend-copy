package com.livescore.backend.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper req =
                new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res =
                new ContentCachingResponseWrapper(response);

        filterChain.doFilter(req, res);

        String requestBody = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
        String responseBody = new String(res.getContentAsByteArray(), StandardCharsets.UTF_8);

        if (!requestBody.isBlank() || !responseBody.isBlank()) {
            log.info("WRITE {} {}", request.getMethod(), request.getRequestURI());
            log.info("REQUEST  : {}", requestBody);
            log.info("RESPONSE : {}", responseBody);
            log.info("STATUS   : {}", response.getStatus());
        }

        res.copyBodyToResponse();
    }
}

