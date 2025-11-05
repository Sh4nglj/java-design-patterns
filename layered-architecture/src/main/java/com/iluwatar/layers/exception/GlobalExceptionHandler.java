package com.iluwatar.layers.exception;

import exception.CakeBakingException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles NullPointerException.
   *
   * @param ex the exception
   * @return ResponseEntity with error message and status
   */
  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<String> handleNullPointerException(NullPointerException ex) {
    return new ResponseEntity<>("Null pointer exception occurred: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles DataAccessException.
   *
   * @param ex the exception
   * @return ResponseEntity with error message and status
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<String> handleDataAccessException(DataAccessException ex) {
    return new ResponseEntity<>("Database access exception occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles CakeBakingException.
   *
   * @param ex the exception
   * @return ResponseEntity with error message and status
   */
  @ExceptionHandler(exception.CakeBakingException.class)
  public ResponseEntity<String> handleCakeBakingException(exception.CakeBakingException ex) {
    return new ResponseEntity<>("Cake baking exception occurred: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles all other exceptions.
   *
   * @param ex the exception
   * @return ResponseEntity with error message and status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleAllExceptions(Exception ex) {
    return new ResponseEntity<>("Unexpected exception occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}