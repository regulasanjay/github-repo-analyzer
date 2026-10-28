package com.githubassistant.api;
import org.springframework.dao.DataAccessException;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().findFirst().map(error->error.getField()+": "+error.getDefaultMessage()).orElse("Invalid request");return ResponseEntity.badRequest().body(Map.of("error",message));}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> handleInvalidRequest(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid request":e.getMessage()));}
 @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> handleDatabaseUnavailable(DataAccessException e){return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error","The configured database is unavailable. Check PostgreSQL and DB_URL, then retry."));}
 @ExceptionHandler(Exception.class) public ResponseEntity<?> handle(Exception e){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error",e.getMessage()==null?"Unexpected server error":e.getMessage()));}
}
