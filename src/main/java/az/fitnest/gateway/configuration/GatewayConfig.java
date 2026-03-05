package az.fitnest.gateway.configuration;

import az.fitnest.gateway.util.RequestUtils;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver keyResolver() {
        return exchange -> Mono.just(RequestUtils.extractClientIP(exchange.getRequest()));
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
