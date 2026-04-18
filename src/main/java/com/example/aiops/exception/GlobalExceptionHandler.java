package com.example.aiops.exception;

import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<UnifiedResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(UnifiedResponse.fail(ex.getCode(), ex.getMessage(), TraceIdHolder.getTraceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UnifiedResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(UnifiedResponse.fail(40001, message, TraceIdHolder.getTraceId()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<UnifiedResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(UnifiedResponse.fail(40002, ex.getMessage(), TraceIdHolder.getTraceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UnifiedResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UnifiedResponse.fail(50000, ex.getMessage(), TraceIdHolder.getTraceId()));
    }
}
