package ru.itmo.dentistry.catalog.common;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    Mono<ApiError> handleNotFound(NotFoundException exception, ServerWebExchange exchange) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    Mono<ApiError> handleConflict(Exception exception, ServerWebExchange exchange) {
        var message = exception instanceof ConflictException
                ? exception.getMessage()
                : "The request conflicts with existing data";
        return error(HttpStatus.CONFLICT, message, exchange, Map.of());
    }

    @ExceptionHandler({BusinessValidationException.class, ConstraintViolationException.class})
    Mono<ApiError> handleBadRequest(Exception exception, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(ServerWebInputException.class)
    Mono<ApiError> handleMalformedRequest(ServerWebInputException exception, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request or unsupported value", exchange, Map.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    Mono<ApiError> handleValidation(WebExchangeBindException exception, ServerWebExchange exchange) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", exchange, fields);
    }

    private Mono<ApiError> error(
            HttpStatus status, String message, ServerWebExchange exchange, Map<String, String> fields) {
        exchange.getResponse().setStatusCode(status);
        return Mono.just(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value(), fields));
    }
}
