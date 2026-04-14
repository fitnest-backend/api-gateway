package az.fitnest.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(key)
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
            return Mono.just(new TokenValidationResult(false, null, null, null, null, null));
        }

        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            if (claims.getExpiration() != null && claims.getExpiration().before(new java.util.Date())) {
                return Mono.just(new TokenValidationResult(false, null, null, null, null, null));
            }

            String subjectStr = claims.getSubject();
            Long parsedUserId = null;
            if (subjectStr != null && !subjectStr.isEmpty()) {
                try {
                    parsedUserId = Long.parseLong(subjectStr);
                } catch (NumberFormatException e) {

                    parsedUserId = claims.get("userId", Long.class);
                }
            }
            final Long userId = parsedUserId;

            final String language = claims.get("lang", String.class);
            final String email = claims.get("email", String.class);
            final String jti = claims.get("jti", String.class);
            @SuppressWarnings("unchecked") final List<String> roles = claims.get("roles", List.class);

            if (checkBlacklist && jti != null) {
                String blacklistKey = "blacklist:jti:" + jti;
                return redisTemplate.hasKey(blacklistKey)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                return Mono.just(new TokenValidationResult(false, null, null, null, null, null));
                            }
                            if (checkSession && userId != null && jti != null) {
                                return validateSession(userId, jti, email, roles, language);
                            }
                            return Mono.just(new TokenValidationResult(true, email, userId, roles, jti, language));
                        });
            }

            if (checkSession && userId != null && jti != null) {
                return validateSession(userId, jti, email, roles, language);
            }

            return Mono.just(new TokenValidationResult(true, email, userId, roles, jti, language));

        } catch (Exception e) {
            return Mono.just(new TokenValidationResult(false, null, null, null, null, null));
        }
    }

    private Mono<TokenValidationResult> validateSession(Long userId, String jti, String email, List<String> roles, String language) {
        String sessionKey = sessionPrefix + userId;
        return redisTemplate.opsForValue().get(sessionKey)
                .map(activeJti -> {
                    if (jti.equals(activeJti)) {
                        return new TokenValidationResult(true, email, userId, roles, jti, language);
                    }
                    return new TokenValidationResult(false, null, null, null, null, null);
                })
                .defaultIfEmpty(new TokenValidationResult(false, null, null, null, null, null));
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
        public final String language;

        public TokenValidationResult(boolean valid, String email, Long userId, List<String> roles, String jti, String language) {
            this.valid = valid;
            this.email = email;
            this.userId = userId;
            this.roles = roles;
            this.jti = jti;
            this.language = language;
        }
    }
}
