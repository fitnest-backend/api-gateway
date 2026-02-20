package az.fitnest.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreProxyController {

    private static final Logger log = LoggerFactory.getLogger(StoreProxyController.class);

    private final WebClient webClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public StoreProxyController(@Value("${MARKETPLACE_SERVICE_URL:http://marketplace-service:8080}") String marketplaceBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(marketplaceBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @GetMapping
    public Mono<ResponseEntity<String>> listStores(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/stores")
                        .queryParam("page", page)
                        .queryParam("pageSize", pageSize)
                        .build())
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    // Proxy for admin endpoints remains
    @PostMapping("/admin")
    public Mono<ResponseEntity<String>> createStoreAdmin(@RequestBody String body, @RequestHeader Map<String, String> headers) {
        return webClient.post()
                .uri("/api/v1/stores/admin")
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @PutMapping("/admin/{storeId}")
    public Mono<ResponseEntity<String>> updateStoreAdmin(@PathVariable String storeId, @RequestBody String body, @RequestHeader Map<String, String> headers) {
        return webClient.put()
                .uri("/api/v1/stores/admin/{id}", storeId)
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @DeleteMapping("/admin/{storeId}")
    public Mono<ResponseEntity<String>> deleteStoreAdmin(@PathVariable String storeId, @RequestHeader Map<String, String> headers) {
        return webClient.delete()
                .uri("/api/v1/stores/admin/{id}", storeId)
                .headers(h -> copyRelevantHeaders(h, headers))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    // Mapping for marketplace main page: /market (proxies to marketplace /api/v1/stores/market)
    @GetMapping("/market")
    public Mono<ResponseEntity<String>> getMarketStores(@RequestHeader Map<String, String> headers, @RequestParam(value = "q", required = false) String q) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/stores/market").queryParamIfPresent("q", java.util.Optional.ofNullable(q)).build())
                .headers(h -> copyRelevantHeaders(h, headers))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @PostMapping("/{storeId}/favorite")
    public Mono<ResponseEntity<String>> toggleFavorite(@PathVariable String storeId, @RequestHeader Map<String, String> headers) {
        return webClient.post()
                .uri("/api/v1/stores/{id}/favorite", storeId)
                .headers(h -> copyRelevantHeaders(h, headers))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    private void copyRelevantHeaders(org.springframework.http.HttpHeaders target, Map<String, String> headers) {
        // Copy Authorization and other non-sensitive user headers so marketplace receives identity.
        if (headers.containsKey("authorization")) {
            target.set(HttpHeaders.AUTHORIZATION, headers.get("authorization"));
        }
        // Do NOT forward X-User-Id header anymore; marketplace should extract user id from the JWT Authorization header.
        // Do not forward X-User-Id under any circumstances.
        if (headers.containsKey("x-user-email")) {
            target.set("X-User-Email", headers.get("x-user-email"));
        }
        if (headers.containsKey("x-user-roles")) {
            target.set("X-User-Roles", headers.get("x-user-roles"));
        }
    }
}
