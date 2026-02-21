package az.fitnest.gateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import az.fitnest.gateway.grpc.StoreGrpcClient;
import az.fitnest.gateway.service.JwtProcessor;
import az.fitnest.gateway.web.support.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreProxyController {

    private final StoreGrpcClient storeGrpcClient;
    private final JwtProcessor jwtProcessor;

    @GetMapping
    public Mono<ResponseEntity<String>> getMarketStores(ServerWebExchange exchange, 
                                                         @RequestParam(value = "q", required = false) String q,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        return extractUserId(exchange)
                .map(userId -> storeGrpcClient.getMarketStores(q, page, pageSize, userId))
                .map(json -> ResponseEntity.ok().header("Content-Type", "application/json").body(json));
    }

    @PostMapping("/admin")
    public Mono<ResponseEntity<String>> createStoreAdmin(@RequestBody String body) {
        return Mono.fromCallable(() -> {
            String json = storeGrpcClient.createStoreAdmin(body);
            return ResponseEntity.status(201).header("Content-Type", "application/json").body(json);
        });
    }

    @PutMapping("/admin/{storeId}")
    public Mono<ResponseEntity<String>> updateStoreAdmin(@PathVariable String storeId, @RequestBody String body) {
        return Mono.fromCallable(() -> {
            String json = storeGrpcClient.updateStoreAdmin(storeId, body);
            return ResponseEntity.ok().header("Content-Type", "application/json").body(json);
        });
    }

    @DeleteMapping("/admin/{storeId}")
    public Mono<ResponseEntity<Void>> deleteStoreAdmin(@PathVariable String storeId) {
        return Mono.fromCallable(() -> {
            storeGrpcClient.deleteStoreAdmin(storeId);
            return ResponseEntity.noContent().build();
        });
    }

    @GetMapping("/market")
    public Mono<ResponseEntity<String>> getMarketStoresAlias(ServerWebExchange exchange, 
                                                             @RequestParam(value = "q", required = false) String q,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return getMarketStores(exchange, q, page, pageSize);
    }

    @PostMapping("/{storeId}/favorite")
    public Mono<ResponseEntity<String>> toggleFavorite(@PathVariable String storeId, ServerWebExchange exchange) {
        return extractUserId(exchange)
                .map(userId -> {
                    storeGrpcClient.toggleFavorite(storeId, userId);
                    return ResponseEntity.ok().body("{\"success\":true}");
                });
    }

    private Mono<Long> extractUserId(ServerWebExchange exchange) {
        String token = RequestUtils.extractToken(exchange);
        if (token == null || token.isBlank()) {
            return Mono.just(0L); // Anonymous
        }
        return jwtProcessor.validateTokenForGateway(token)
                .map(validation -> {
                    if (validation.valid && validation.userId != null) {
                        return validation.userId;
                    }
                    return 0L;
                })
                .onErrorReturn(0L);
    }
}
