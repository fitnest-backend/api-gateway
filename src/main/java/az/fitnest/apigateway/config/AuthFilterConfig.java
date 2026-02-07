package az.fitnest.apigateway.config;

import az.fitnest.apigateway.service.BlockManager;
import az.fitnest.apigateway.service.JwtProcessor;
import az.fitnest.apigateway.service.RateLimiter;
import az.fitnest.apigateway.security.support.CsrfValidator;
import az.fitnest.apigateway.security.support.PathValidator;
import az.fitnest.apigateway.security.support.SecurityHeaders;
import az.fitnest.apigateway.web.support.RequestUtils;
import az.fitnest.apigateway.web.support.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class AuthFilterConfig {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private BlockManager blockManager;

    @Autowired
    private JwtProcessor jwtProcessor;

    @Bean
    public GlobalFilter authFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // Bypass auth and rate limiting for OpenAPI docs and Swagger UI
            if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/swagger")) {
                return chain.filter(exchange);
            }

            String method = exchange.getRequest().getMethod().name();
            String clientIP = RequestUtils.extractClientIP(exchange.getRequest());

            return rateLimiter.checkRateLimit(clientIP, path, method)
                    .flatMap(result -> {
                        if (result == -1) {
                            return ResponseUtils.respondWithTooManyRequests(exchange.getResponse());
                        }

                        return proceedWithAuth(exchange, chain, path, method, clientIP);
                    });
        };
    }

    private Mono<Void> proceedWithAuth(ServerWebExchange exchange, GatewayFilterChain chain, String path, String method, String clientIP) {
        String token = RequestUtils.extractToken(exchange);
        boolean requiresAuth = PathValidator.requiresAuth(path);

        if (requiresAuth && (token == null || token.isEmpty())) {
            return handleAuthFailure(exchange, clientIP, path);
        }

        if (token != null && !token.isEmpty()) {
            return jwtProcessor.validateTokenForGateway(token)
                    .flatMap(validation -> handleTokenValidation(exchange, chain, validation, requiresAuth, clientIP, path, token));
        }

        return chain.filter(exchange);
    }

    private Mono<Void> handleAuthFailure(ServerWebExchange exchange, String clientIP, String path) {
        Mono<Void> recordMono = path.equals("/api/v1/auth/verify-otp") ?
            Mono.empty() : blockManager.recordFailedAttempt(clientIP, null).then();
        return recordMono.then(ResponseUtils.respondWithUnauthorized(exchange.getResponse()));
    }

    private Mono<Void> handleTokenValidation(ServerWebExchange exchange, GatewayFilterChain chain,
                                           JwtProcessor.TokenValidationResult validation, boolean requiresAuth,
                                           String clientIP, String path, String token) {
        if (!validation.valid) {
            if (requiresAuth) {
                return handleAuthFailure(exchange, clientIP, path);
            } else {
                return chain.filter(exchange);
            }
        }

        return blockManager.isBlocked(clientIP, validation.email)
                .flatMap(blocked -> {
                    if (blocked) {
                        return ResponseUtils.respondWithForbidden(exchange.getResponse());
                    }

                    ServerWebExchange modifiedExchange = addUserHeaders(exchange, token, validation);
                    return chain.filter(modifiedExchange);
                });
    }

    private ServerWebExchange addUserHeaders(ServerWebExchange exchange, String token, JwtProcessor.TokenValidationResult validation) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Email", validation.email)
                        .header("X-User-Id", validation.userId != null ? validation.userId.toString() : "")
                        .header("X-User-Roles", validation.roles != null ? String.join(",", validation.roles) : "")
                        .build())
                .build();
    }


    @Bean
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            SecurityHeaders.addSecurityHeaders(exchange.getResponse());

            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            return CsrfValidator.validateCsrfToken(exchange, path, method)
                    .then(chain.filter(exchange));
        };
    }

}
