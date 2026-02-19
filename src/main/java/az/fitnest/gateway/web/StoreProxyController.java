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

    @GetMapping("/all")
    public Mono<ResponseEntity<String>> getAllStores(@RequestHeader Map<String, String> headers) {
        return webClient.get()
                .uri("/api/v1/stores/all")
                .headers(h -> copyRelevantHeaders(h, headers))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @GetMapping("/{storeId}")
    public Mono<ResponseEntity<String>> getStore(@PathVariable String storeId, @RequestHeader Map<String, String> headers) {
        return webClient.get()
                .uri("/api/v1/stores/{id}", storeId)
                .headers(h -> copyRelevantHeaders(h, headers))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @PostMapping
    public Mono<ResponseEntity<String>> createStore(@RequestBody String body, @RequestHeader Map<String, String> headers) {
        // Log minimal info to help debug bad requests
        log.debug("Proxying createStore body length={} headersContainsAuthorization={}",
                body != null ? body.length() : 0,
                headers != null && headers.containsKey("authorization"));

        // Basic validation: ensure body is valid JSON and contains non-blank 'name'
        if (body == null || body.isBlank()) {
            String errorJson = "{\"error\":{\"message\":\"Request body required\"}}";
            return Mono.just(ResponseEntity.badRequest().body(errorJson));
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode nameNode = root.get("name");
            if (nameNode == null || nameNode.asText().isBlank()) {
                String errorJson = "{\"error\":{\"message\":\"Field 'name' is required\"}}";
                log.debug("Rejecting createStore: missing name in body={}", body);
                return Mono.just(ResponseEntity.badRequest().body(errorJson));
            }
        } catch (Exception e) {
            String errorJson = "{\"error\":{\"message\":\"Invalid JSON body\"}}";
            log.debug("Rejecting createStore: invalid JSON body", e);
            return Mono.just(ResponseEntity.badRequest().body(errorJson));
        }

        return webClient.post()
                .uri("/api/v1/stores")
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(response -> response.toEntity(String.class));
    }

    @PostMapping("/admin")
    public Mono<ResponseEntity<String>> createStoreAdmin(@RequestBody String body, @RequestHeader Map<String, String> headers) {
        return webClient.post()
                .uri("/api/v1/stores/admin")
                .headers(h -> copyRelevantHeaders(h, headers))
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(response -> response.toEntity(String.class));
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
