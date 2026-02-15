package com.automation.taskplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Determine appropriate status code based on message
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (message != null) {
            if (message.contains("already in use") ||
                message.contains("Invalid") ||
                message.contains("not found")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }

        return ResponseEntity.status(status)
            .body(Map.of("error", message != null ? message : "An error occurred"));
    }
}
