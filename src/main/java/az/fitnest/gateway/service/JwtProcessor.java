package az.fitnest.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;
import java.util.UUID;

@Component
public class JwtProcessor {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final String sessionPrefix;
    @Value("${JWT_SECRET:your-jwt-secret-change-in-production}")
    private String secretKey;
    private Key signingKey;
    private io.jsonwebtoken.JwtParser jwtParser;

    public JwtProcessor(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${security.redis.session-prefix:auth:user:session:}") String sessionPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.sessionPrefix = sessionPrefix;
    }

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build();
    }

    public String generateJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Mono<TokenValidationResult> validateTokenForGateway(String token) {
        return validateTokenInternal(token, false, false);
    }

    public Mono<TokenValidationResult> validateToken(String token) {
        return validateTokenInternal(token, true, true);
    }

    private Mono<TokenValidationResult> validateTokenInternal(String token, boolean checkBlacklist, boolean checkSession) {
        if (token == null || token.isEmpty()) {
            return Mono.just(new TokenValidationResult(false, null, null, null, null));
        }

        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();

            if (claims.getExpiration() != null && claims.getExpiration().before(new java.util.Date())) {
                return Mono.just(new TokenValidationResult(false, null, null, null, null));
            }

            // IAM service puts userId in 'sub' claim (as string), not email
            String subjectStr = claims.getSubject();
            Long parsedUserId = null;
            if (subjectStr != null && !subjectStr.isEmpty()) {
                try {
                    parsedUserId = Long.parseLong(subjectStr);
                } catch (NumberFormatException e) {
                    // If sub is not a number, it might be an email (legacy format)
                    // In that case, try to get userId from separate claim
                    parsedUserId = claims.get("userId", Long.class);
                }
            }
            // Final variable for use in lambda
            final Long userId = parsedUserId;

            // Email may or may not be present in the token
            final String email = claims.get("email", String.class);
            final String jti = claims.get("jti", String.class);
            @SuppressWarnings("unchecked") final List<String> roles = claims.get("roles", List.class);

            if (checkBlacklist && jti != null) {
                String blacklistKey = "blacklist:jti:" + jti;
                return redisTemplate.hasKey(blacklistKey)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                return Mono.just(new TokenValidationResult(false, null, null, null, null));
                            }
                            if (checkSession && userId != null && jti != null) {
                                return validateSession(userId, jti, email, roles);
                            }
                            return Mono.just(new TokenValidationResult(true, email, userId, roles, jti));
                        });
            }

            if (checkSession && userId != null && jti != null) {
                return validateSession(userId, jti, email, roles);
            }

            return Mono.just(new TokenValidationResult(true, email, userId, roles, jti));

        } catch (Exception e) {
            return Mono.just(new TokenValidationResult(false, null, null, null, null));
        }
    }

    private Mono<TokenValidationResult> validateSession(Long userId, String jti, String email, List<String> roles) {
        String sessionKey = sessionPrefix + userId;
        return redisTemplate.opsForValue().get(sessionKey)
                .map(activeJti -> {
                    if (jti.equals(activeJti)) {
                        return new TokenValidationResult(true, email, userId, roles, jti);
                    }
                    return new TokenValidationResult(false, null, null, null, null);
                })
                .defaultIfEmpty(new TokenValidationResult(false, null, null, null, null));
    }

    public boolean isAdminRoute(String path) {
        return path.startsWith("/api/v1/admin/") &&
                !path.equals("/api/v1/admin/create-admin");
    }

    public static class TokenValidationResult {
        public final boolean valid;
        public final String email;
        public final Long userId;
        public final List<String> roles;
        public final String jti;

        public TokenValidationResult(boolean valid, String email, Long userId, List<String> roles, String jti) {
            this.valid = valid;
            this.email = email;
            this.userId = userId;
            this.roles = roles;
            this.jti = jti;
        }
    }
}
