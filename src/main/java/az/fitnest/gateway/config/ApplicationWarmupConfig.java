package az.fitnest.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Async;


@Configuration
public class ApplicationWarmupConfig {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

    public ApplicationWarmupConfig(@org.springframework.beans.factory.annotation.Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
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
            long start = System.currentTimeMillis();
            redisTemplate.hasKey("__warmup__").subscribe();
        } catch (Exception e) {
        }
    }

    private void warmupJit() {
        try {
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

        } catch (Exception e) {
        }
    }
}
