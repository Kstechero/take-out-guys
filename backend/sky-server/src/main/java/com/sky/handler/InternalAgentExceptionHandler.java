package com.sky.handler;

import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.sky.controller.internal")
public class InternalAgentExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> forbidden(SecurityException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Actor is not allowed", request);
    }

    @ExceptionHandler(OrderBusinessException.class)
    public ResponseEntity<Map<String, Object>> orderNotFound(OrderBusinessException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, BaseException.class})
    public ResponseEntity<Map<String, Object>> validation(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internal(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal agent API failed", request);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message,
                                                       HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("data", null);
        body.put("error_code", code);
        body.put("message", message);
        body.put("request_id", request.getHeader("X-Request-Id"));
        return ResponseEntity.status(status).body(body);
    }
}
