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
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Suspicious path patterns that indicate security scans or attacks
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
        return ErrorResponseBuilder.internalServerError("API Gateway-də gözlənilməz xəta baş verdi", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleServerWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest("Yanlış sorğu formatı", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest(ex.getMessage(), exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ApiError> handleConnectException(ConnectException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("Sorğu edilən xidmət hazırda əlçatan deyil. Zəhmət olmasa bir az sonra yenidən yoxlayın.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity<ApiError> handleUnknownHostException(UnknownHostException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("Sorğu edilən xidmət tapılmadı. Ola bilsin ki, xidmət sönülüdür və ya daxil olmaq mümkün deyil.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiError> handleTimeoutException(TimeoutException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.gatewayTimeout("Xidmətin cavabı gözlənilərkən vaxt bitdi.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiError> handleWebClientRequestException(WebClientRequestException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.serviceUnavailable("Sorğu edilən xidmətə qoşulmaq mümkün olmadı. Zəhmət olmasa bir az sonra yenidən yoxlayın.", exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiError> handleWebClientResponseException(WebClientResponseException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (ex.getStatusCode().is5xxServerError()) {
            return ErrorResponseBuilder.serviceUnavailable("Xidmətdə problem yarandı. Zəhmət olmasa bir az sonra yenidən yoxlayın.", path);
        } else if (ex.getStatusCode().is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(ex.getStatusText() + ": " + ex.getMessage(), path);
        }
        return ErrorResponseBuilder.internalServerError("Xidmət rabitə xətası", path);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, ServerWebExchange exchange) {
        return ErrorResponseBuilder.badRequest("Sorğu edilən xidmət və ya resurs tapılmadı.", exchange.getRequest().getPath().value());
    }

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
            //
        } else if (path.equals("/") || path.equals("")) {
            // Root path request - common from health checks or misconfigured clients
            //
        } else if (path.startsWith("/api/")) {
            // This is an API path that doesn't match any route - worth noting
            //
        } else {
            //
        }

        return ErrorResponseBuilder.notFound("Sorğu edilən resurs tapılmadı.", path);
    }

    /**
     * Handles ResponseStatusException which can be thrown for various HTTP status scenarios.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        if (status == HttpStatus.NOT_FOUND) {
            //
            return ErrorResponseBuilder.notFound(ex.getReason() != null ? ex.getReason() : "Resurs tapılmadı", path);
        } else if (status.is4xxClientError()) {
            return ErrorResponseBuilder.badRequest(ex.getReason() != null ? ex.getReason() : "Yanlış sorğu", path);
        } else {
            //
            return ErrorResponseBuilder.internalServerError("Sorğunuz emal edilərkən xəta baş verdi", path);
        }
    }

    private boolean isSuspiciousPath(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        return SUSPICIOUS_PATTERNS.stream().anyMatch(lowerPath::contains);
    }
}
