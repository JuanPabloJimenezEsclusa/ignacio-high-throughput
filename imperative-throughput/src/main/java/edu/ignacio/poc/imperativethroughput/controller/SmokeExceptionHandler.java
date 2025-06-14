package edu.ignacio.poc.imperativethroughput.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that returns a structured JSON error body.
 */
@ControllerAdvice
public class SmokeExceptionHandler {

  private static Map<String, Object> buildBody(final String message) {
    return Map.of(
      "status", 500,
      "error", "Internal Server Error",
      "details", message
    );
  }

  /**
   * Maps any {@link RuntimeException} to a 500 response with a JSON body.
   *
   * @param exception uncaught runtime exception
   * @return a 500 {@link ResponseEntity} with a structured JSON error body
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(final RuntimeException exception) {
    return ResponseEntity.internalServerError()
      .contentType(MediaType.APPLICATION_JSON)
      .body(buildBody(exception.getMessage()));
  }
}
