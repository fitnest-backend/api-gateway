package az.fitnest.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Async;

/**
 * Application warmup configuration that pre-warms various resources at startup.
 * Eliminates cold-start latency.
 */
@Slf4j
@Configuration
public class ApplicationWarmupConfig {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

    public ApplicationWarmupConfig(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupApplication() {
        if (!warmupEnabled) {
            return;
        }

        warmupRedis();
        warmupJit();
    }

    private void warmupRedis() {
        try {
            log.debug("Warming up Redis connection...");
            long start = System.currentTimeMillis();
            redisTemplate.hasKey("__warmup__").subscribe();
            log.debug("Redis warmup requested in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Failed to warm up Redis: {}", e.getMessage());
        }
    }

    private void warmupJit() {
        try {
            log.debug("Warming up JIT...");
            long start = System.currentTimeMillis();

            String test = "warmup-test-string";
            test.toLowerCase();
            test.toUpperCase();
            test.split("-");

            java.util.List<String> list = new java.util.ArrayList<>();
            list.add("test");
            list.stream().filter(s -> s != null).count();

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("key", "value");
            map.get("key");

            log.debug("JIT warmup completed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Failed JIT warmup: {}", e.getMessage());
        }
    }
}
