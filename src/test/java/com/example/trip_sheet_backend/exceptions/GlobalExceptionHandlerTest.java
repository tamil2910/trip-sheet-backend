package com.example.trip_sheet_backend.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.trip_sheet_backend.response_setups.ApiResponse;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void responseStatusExceptionsPreserveTheirStatusAndReason() {
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid expense type");

    var response = handler.handleResponseStatusException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    ApiResponse<String> body = response.getBody();
    assertEquals("Invalid expense type", body.getMessage());
    assertEquals(false, body.isSuccess());
  }
}
