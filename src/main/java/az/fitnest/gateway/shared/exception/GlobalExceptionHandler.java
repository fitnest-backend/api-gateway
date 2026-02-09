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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, ServerWebExchange exchange) {
        logger.error("Unexpected error in API Gateway: ", ex);
        return ErrorResponseBuilder.internalServerError("An unexpected error occurred in the API Gateway", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleServerWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest("Invalid request format", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest(ex.getMessage(), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ApiError> handleConnectException(ConnectException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("The requested service is currently unavailable. Please try again later.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity<ApiError> handleUnknownHostException(UnknownHostException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("Failed to resolve the requested service. It may be down or unaccessible.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiError> handleTimeoutException(TimeoutException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.gatewayTimeout("The request timed out while waiting for the service to respond.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiError> handleWebClientRequestException(WebClientRequestException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("Unable to connect to the requested service. Please try again later.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiError> handleWebClientResponseException(WebClientResponseException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (ex.getStatusCode().is5xxServerError()) {
            return ErrorResponseBuilder.serviceUnavailable("The service is experiencing issues. Please try again later.", path);
        } else if (ex.getStatusCode().is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(ex.getStatusText() + ": " + ex.getMessage(), path);
        }
        return ErrorResponseBuilder.internalServerError("Service communication error", path);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest("The requested service or resource was not found.", exchange.getRequest().getPath().value());
    }

    // Suspicious path patterns that indicate security scans or attacks
    private static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
            ".env", ".git", ".svn", ".htaccess", ".htpasswd",
            "wp-admin", "wp-login", "wp-content", "wordpress",
            "admin", "phpmyadmin", "mysql", "sql",
            "bin/sh", "bin/bash", "etc/passwd", "etc/shadow",
            "cmd.exe", "powershell", "shell", "cgi-bin",
            ".aws", ".docker", "config.json", "credentials"
    );

    /**
     * Handles requests for static resources that don't exist.
     * This catches:
     * 1. Root path requests (/)
     * 2. Security scan attempts (/.env, /bin/sh, etc.)
     * 3. Any other unmatched paths that fall through to the static resource handler
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        
        // Check if this looks like a security scan - log at debug level only
        if (isSuspiciousPath(path)) {
            logger.debug("Blocked suspicious request to: {}", path);
        } else if (path.equals("/") || path.equals("")) {
            // Root path request - common from health checks or misconfigured clients
            logger.debug("Root path request received - no handler configured for /");
        } else if (path.startsWith("/api/")) {
            // This is an API path that doesn't match any route - worth noting
            logger.warn("No route configured for API path: {}", path);
        } else {
            logger.debug("No static resource found for path: {}", path);
        }
        
        return ErrorResponseBuilder.notFound("The requested resource was not found.", path);
    }

    /**
     * Handles ResponseStatusException which can be thrown for various HTTP status scenarios.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        
        if (status == HttpStatus.NOT_FOUND) {
            logger.debug("Resource not found: {}", path);
            return ErrorResponseBuilder.notFound(ex.getReason() != null ? ex.getReason() : "Resource not found", path);
        } else if (status.is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(ex.getReason() != null ? ex.getReason() : "Bad request", path);
        } else {
            logger.error("ResponseStatusException with status {}: {}", status, ex.getMessage());
            return ErrorResponseBuilder.internalServerError("An error occurred processing your request", path);
        }
    }

    private boolean isSuspiciousPath(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return SUSPICIOUS_PATTERNS.stream().anyMatch(lowerPath::contains);
    }
}
