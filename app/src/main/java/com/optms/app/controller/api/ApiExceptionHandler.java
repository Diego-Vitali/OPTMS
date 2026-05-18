package com.optms.app.controller.api;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception) {
        return build(exception.getStatusCode().value(), exception.getReason() != null ? exception.getReason() : "Erro na requisição");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST.value(), message.isBlank() ? "Payload inválido" : message);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST.value(), exception.getMessage() != null ? exception.getMessage() : "Requisição inválida");
    }

    private ResponseEntity<Map<String, Object>> build(int status, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("message", message);
        return ResponseEntity.status(status).body(payload);
    }

    private String formatFieldError(FieldError error) {
        if (error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()) {
            return error.getField() + " inválido";
        }
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
