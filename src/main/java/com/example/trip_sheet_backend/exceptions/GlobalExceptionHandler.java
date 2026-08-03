package com.example.trip_sheet_backend.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.trip_sheet_backend.response_setups.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiResponse<>(false, ex.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getFieldErrors().forEach(error -> {
      errors.put(error.getField(), error.getDefaultMessage());
    });

    return ResponseEntity.badRequest().body(
        new ApiResponse<>(false, "Validation failed", errors)
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    String errorMessage = "Data integrity violation: " + ex.getMostSpecificCause().getMessage();
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiResponse<>(false, errorMessage, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<String>> handleAllUncaughtException(Exception ex) {
    System.out.println("===== FULL ERROR TRACE =====");
    ex.printStackTrace();

    String normalizedMessage = getNormalizedMessage(ex);
    if (normalizedMessage.contains("innodb_force_recovery")) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .header(HttpHeaders.RETRY_AFTER, "60")
          .body(new ApiResponse<>(
              false,
              "Database is running in InnoDB recovery mode and is currently read-only. Disable innodb_force_recovery on the MySQL server before retrying this write operation.",
              null
          ));
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiResponse<>(false, "An unexpected error occurred: " + ex.getMessage(), null));
  }

  private String getNormalizedMessage(Throwable throwable) {
    StringBuilder combinedMessage = new StringBuilder();
    Throwable current = throwable;

    while (current != null) {
      if (current.getMessage() != null && !current.getMessage().isBlank()) {
        if (combinedMessage.length() > 0) {
          combinedMessage.append(' ');
        }
        combinedMessage.append(current.getMessage().toLowerCase());
      }
      current = current.getCause();
    }

    return combinedMessage.toString();
  }
}
