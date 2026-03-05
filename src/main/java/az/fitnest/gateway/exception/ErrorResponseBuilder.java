package az.fitnest.gateway.exception;

import az.fitnest.gateway.exception.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ErrorResponseBuilder {

    private ErrorResponseBuilder() {
    }

    public static ResponseEntity<ApiError> buildError(HttpStatus status, String code, String message, String path) {
        ApiError error = ApiError.builder()
                .code(code)
                .message(message)
                .status(status.value())
                .path(path)
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    public static ResponseEntity<ApiError> buildErrorWithRetryAfter(HttpStatus status, String code, String message, String retryAfter, String path) {
        ApiError error = ApiError.builder()
                .code(code)
                .message(message)
                .status(status.value())
                .path(path)
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status)
                .header("Retry-After", retryAfter)
                .body(error);
    }

    public static ResponseEntity<ApiError> internalServerError(String message, String path) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message, path);
    }

    public static ResponseEntity<ApiError> badRequest(String message, String path) {
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, path);
    }

    public static ResponseEntity<ApiError> serviceUnavailable(String message, String path) {
        return buildErrorWithRetryAfter(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message, "60", path);
    }

    public static ResponseEntity<ApiError> gatewayTimeout(String message, String path) {
        return buildError(HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT", message, path);
    }

    public static ResponseEntity<ApiError> notFound(String message, String path) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", message, path);
    }
}
