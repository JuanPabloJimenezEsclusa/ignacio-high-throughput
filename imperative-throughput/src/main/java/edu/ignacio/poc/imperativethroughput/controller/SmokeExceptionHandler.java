package edu.ignacio.poc.imperativethroughput.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * The type Smoke exception handler.
 */
@ControllerAdvice
public class SmokeExceptionHandler {

  /**
   * Handle exceptions response entity.
   *
   * @param ex the runtimeException
   * @return the response entity
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<String> handleExceptions(final RuntimeException ex) {
    return ResponseEntity.internalServerError().body(ex.getMessage());
  }
}
