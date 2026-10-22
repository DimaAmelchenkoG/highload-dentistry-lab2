package ru.itmo.dentistry.appointment.common;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException exception, ServerWebExchange exchange) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ResponseEntity<ApiError> handleUnavailable(CatalogUnavailableException exception, ServerWebExchange exchange) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler({
            ConflictException.class,
            DataIntegrityViolationException.class,
            PessimisticLockingFailureException.class,
            ObjectOptimisticLockingFailureException.class
    })
    ResponseEntity<ApiError> handleConflict(Exception exception, ServerWebExchange exchange) {
        var message = exception instanceof ConflictException
                ? exception.getMessage()
                : "The request conflicts with existing data";
        return error(HttpStatus.CONFLICT, message, exchange, Map.of());
    }

    @ExceptionHandler({BusinessValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ApiError> handleMalformedRequest(ServerWebInputException exception, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request or unsupported value", exchange, Map.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiError> handleValidation(WebExchangeBindException exception, ServerWebExchange exchange) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", exchange, fields);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String message, ServerWebExchange exchange, Map<String, String> fields) {
        var body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
