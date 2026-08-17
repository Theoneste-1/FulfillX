package com.fulfillx.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillx.common.error.ApiError;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.web.Headers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(
                Instant.now(),
                401,
                "Unauthorized",
                ErrorCode.UNAUTHENTICATED.name(),
                "Authentication required",
                request.getRequestURI(),
                request.getHeader(Headers.CORRELATION_ID),
                null
        );
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
