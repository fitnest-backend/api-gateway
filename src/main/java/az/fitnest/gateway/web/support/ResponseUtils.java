package az.fitnest.gateway.web.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import az.fitnest.gateway.shared.exception.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResponseUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ResponseUtils() {
    }

    public static Mono<Void> respondWithStatus(ServerHttpResponse response, HttpStatus status) {
        response.setStatusCode(status);
        return response.setComplete();
    }

    public static Mono<Void> respondWithTooManyRequests(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));
        response.getHeaders().add("Retry-After", "60");

        ApiError error = ApiError.builder()
                .error(ApiError.ErrorDetail.builder()
                        .code("TOO_MANY_REQUESTS")
                        .message("Rate limit exceeded. Please wait 1 minute before making another request.")
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .path(exchange.getRequest().getPath().value())
                        .build())
                .build();

        try {
            String jsonResponse = OBJECT_MAPPER.writeValueAsString(error);
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    public static Mono<Void> respondWithForbidden(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiError error = ApiError.builder()
                .error(ApiError.ErrorDetail.builder()
                        .code("FORBIDDEN")
                        .message("Access is forbidden (possible block or insufficient permissions)")
                        .status(HttpStatus.FORBIDDEN.value())
                        .path(exchange.getRequest().getPath().value())
                        .build())
                .build();
        try {
            String json = OBJECT_MAPPER.writeValueAsString(error);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    public static Mono<Void> respondWithUnauthorized(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiError error = ApiError.builder()
                .error(ApiError.ErrorDetail.builder()
                        .code("UNAUTHORIZED")
                        .message("Authentication required")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .path(exchange.getRequest().getPath().value())
                        .build())
                .build();
        try {
            String json = OBJECT_MAPPER.writeValueAsString(error);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
