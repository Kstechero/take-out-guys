package com.sky.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.context.BaseContext;
import com.sky.properties.AgentServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@Component
public class InternalAgentAuthInterceptor implements HandlerInterceptor {

    private static final String SERVICE_TOKEN_HEADER = "X-Agent-Service-Token";

    private final AgentServiceProperties properties;
    private final ObjectMapper objectMapper;

    public InternalAgentAuthInterceptor(AgentServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String expected = properties.getInternalAuthToken();
        String actual = request.getHeader(SERVICE_TOKEN_HEADER);
        if (!StringUtils.hasText(expected) || !expected.equals(actual)) {
            return reject(request, response, "UNAUTHENTICATED", "Invalid internal agent service token");
        }

        String actorType = request.getHeader("X-Actor-Type");
        String actorId = request.getHeader("X-Actor-Id");
        String actorRoles = request.getHeader("X-Actor-Roles");
        String expiresAt = request.getHeader("X-Actor-Expires-At");
        Long parsedActorId = parseActorId(actorId);
        if (!("user".equals(actorType) || "admin".equals(actorType))
                || parsedActorId == null
                || !StringUtils.hasText(actorRoles)
                || !isFutureInstant(expiresAt)) {
            return reject(request, response, "FORBIDDEN", "Invalid or expired actor context");
        }
        BaseContext.setCurrentId(parsedActorId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }

    private Long parseActorId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isFutureInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            return Instant.parse(value).isAfter(Instant.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response,
                           String errorCode, String message) throws Exception {
        response.setStatus("FORBIDDEN".equals(errorCode)
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", false);
        payload.put("data", null);
        payload.put("error_code", errorCode);
        payload.put("message", message);
        payload.put("request_id", request.getHeader("X-Request-Id"));
        objectMapper.writeValue(response.getWriter(), payload);
        return false;
    }
}
