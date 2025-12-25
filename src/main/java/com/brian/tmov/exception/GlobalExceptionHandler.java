package com.brian.tmov.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(-2)
public class GlobalExceptionHandler {

    // 處理參數驗證錯誤 (@Valid)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                message
        );
        log.warn("Validation failed: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 處理業務邏輯錯誤 (例如：帳號已存在、找不到帳號)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER",
                ex.getMessage()
        );
        log.warn("Invalid parameter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 處理 TMDB 連線錯誤
    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamApiException(DownstreamException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "DOWNSTREAM_API_FAILED",
                ex.getMessage()
        );
        log.warn("Downstream API call failed: {}", ex.getMessage(), ex.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    // 處理密碼錯誤
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 其他未預期錯誤
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "系統發生未預期錯誤"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
