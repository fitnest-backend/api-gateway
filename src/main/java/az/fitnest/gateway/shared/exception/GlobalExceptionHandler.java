package az.fitnest.gateway.shared.exception;

import az.fitnest.gateway.shared.exception.support.ErrorResponseBuilder;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import org.springframework.context.MessageSource;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
            ".env", ".git", ".svn", ".htaccess", ".htpasswd",
            "wp-admin", "wp-login", "wp-content", "wordpress",
            "admin", "phpmyadmin", "mysql", "sql",
            "bin/sh", "bin/bash", "etc/passwd", "etc/shadow",
            "cmd.exe", "powershell", "shell", "cgi-bin",
            ".aws", ".docker", "config.json", "credentials"
    );

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.internalServerError(getMessage("error.gateway_unexpected", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleServerWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest(getMessage("error.invalid_json_format", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest(safeMessage(ex.getMessage(), exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ApiError> handleConnectException(ConnectException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable(getMessage("error.service_unavailable", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity<ApiError> handleUnknownHostException(UnknownHostException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable(getMessage("error.service_not_found", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiError> handleTimeoutException(TimeoutException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.gatewayTimeout(getMessage("error.gateway_timeout", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiError> handleWebClientRequestException(WebClientRequestException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable(getMessage("error.connection_failed", exchange), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiError> handleWebClientResponseException(WebClientResponseException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (ex.getStatusCode().is5xxServerError()) {
            return ErrorResponseBuilder.serviceUnavailable(getMessage("error.service_problem", exchange), path);
        } else if (ex.getStatusCode().is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(getMessage("error.bad_request", exchange), path);
        }
        return ErrorResponseBuilder.internalServerError(getMessage("error.communication_error", exchange), path);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest(getMessage("error.resource_not_found", exchange), exchange.getRequest().getPath().value());
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        if (isSuspiciousPath(path)) {
        } else if (path.equals("/") || path.equals("")) {

        } else if (path.startsWith("/api/")) {

        } else {

        }

        return ErrorResponseBuilder.notFound(getMessage("error.not_found", exchange), path);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        if (status == HttpStatus.NOT_FOUND) {
            return ErrorResponseBuilder.notFound(ex.getReason() != null ? ex.getReason() : getMessage("error.not_found", exchange), path);
        } else if (status.is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(ex.getReason() != null ? ex.getReason() : getMessage("error.bad_request", exchange), path);
        } else {

            return ErrorResponseBuilder.internalServerError(getMessage("error.generic_request_error", exchange), path);
        }
    }

    private String getMessage(String code, ServerWebExchange exchange) {
        try {
            Locale locale = exchange.getLocaleContext().getLocale();
            return messageSource.getMessage(code, null, locale != null ? locale : Locale.ENGLISH);
        } catch (Exception e) {
            return code;
        }
    }

    private String safeMessage(String msg, ServerWebExchange exchange) {
        if (msg == null || msg.isBlank()) {
            return getMessage("error.gateway_unexpected", exchange);
        }
        if (msg.startsWith("error.")) {
            String resolved = getMessage(msg, exchange);
            if (!resolved.equals(msg)) {
                return resolved;
            }
        }
        return msg;
    }

    private boolean isSuspiciousPath(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return SUSPICIOUS_PATTERNS.stream().anyMatch(lowerPath::contains);
    }
}
