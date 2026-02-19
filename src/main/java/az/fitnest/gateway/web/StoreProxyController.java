package az.fitnest.gateway.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreProxyController {

    private final WebClient webClient;

    public StoreProxyController(@Value("${MARKETPLACE_SERVICE_URL:http://marketplace-service:8080}") String marketplaceBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(marketplaceBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @GetMapping
    public Mono<ResponseEntity<String>> listStores(@RequestHeader Map<String, String> headers, @RequestParam Map<String, String> params) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var ub = uriBuilder.path("/api/v1/stores");
                    params.forEach(ub::queryParam);
                    return ub.build();
                })
                .headers(h -> copyRelevantHeaders(h, headers))
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/{storeId}")
    public Mono<ResponseEntity<String>> getStore(@PathVariable String storeId, @RequestHeader Map<String, String> headers) {
        return webClient.get()
                .uri("/api/v1/stores/{id}", storeId)
                .headers(h -> copyRelevantHeaders(h, headers))
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping
    public Mono<ResponseEntity<String>> createStore(@RequestBody String body, @RequestHeader Map<String, String> headers) {
        return webClient.post()
                .uri("/api/v1/stores")
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping("/admin")
    public Mono<ResponseEntity<String>> createStoreAdmin(@RequestBody String body, @RequestHeader Map<String, String> headers) {
        return webClient.post()
                .uri("/api/v1/stores/admin")
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toEntity(String.class);
    }

    private void copyRelevantHeaders(HttpHeaders target, Map<String, String> headers) {
        // Copy Authorization and user headers so marketplace receives identity
        if (headers.containsKey("authorization")) {
            target.set(HttpHeaders.AUTHORIZATION, headers.get("authorization"));
        }
        if (headers.containsKey("x-user-id")) {
            target.set("X-User-Id", headers.get("x-user-id"));
        }
        if (headers.containsKey("x-user-email")) {
            target.set("X-User-Email", headers.get("x-user-email"));
        }
        if (headers.containsKey("x-user-roles")) {
            target.set("X-User-Roles", headers.get("x-user-roles"));
        }
    }
}
