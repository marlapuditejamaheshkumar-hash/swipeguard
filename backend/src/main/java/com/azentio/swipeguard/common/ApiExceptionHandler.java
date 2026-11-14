package com.azentio.swipeguard.common;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record FieldProblem(String field, String message) { }

    public record ErrorBody(String error, List<FieldProblem> fields) { }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalidBody(MethodArgumentNotValidException ex) {
        List<FieldProblem> problems = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldProblem(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_FAILED", problems));
    }

    /** Broken JSON or an unknown enum value. The message is generic on purpose: it could contain the PAN. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorBody("MALFORMED_REQUEST", List.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorBody> badParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_FAILED",
                List.of(new FieldProblem(ex.getName(), "has an invalid value"))));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorBody> badRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_FAILED",
                List.of(new FieldProblem(ex.getField(), ex.getMessage()))));
    }
}